package com.rajesh.milansetu.service;

import com.rajesh.milansetu.dto.request.MemberRequest;
import com.rajesh.milansetu.dto.response.MemberResponse;
import com.rajesh.milansetu.entity.Event;
import com.rajesh.milansetu.entity.Member;
import com.rajesh.milansetu.exception.BusinessRuleException;
import com.rajesh.milansetu.exception.ResourceNotFoundException;
import com.rajesh.milansetu.repository.MemberRepository;
import com.rajesh.milansetu.repository.UserRepository;
import com.rajesh.milansetu.security.AccessPolicy;
import com.rajesh.milansetu.security.AppUserPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import java.time.LocalDate;
import java.util.Base64;
import java.util.List;

@Service
public class MemberService {
    private final MemberRepository members;
    private final UserRepository users;
    private final EventService events;
    private final QrService qr;
    private final AccessPolicy access;
    private final AuditService audit;

    public MemberService(MemberRepository members, UserRepository users, EventService events, QrService qr,
                         AccessPolicy access, AuditService audit) {
        this.members = members;
        this.users = users;
        this.events = events;
        this.qr = qr;
        this.access = access;
        this.audit = audit;
    }

    @Transactional
    public MemberResponse create(MemberRequest req, AppUserPrincipal family) {
        Event event = events.find(req.eventId());
        if (event.getEndDate().isBefore(LocalDate.now())) throw new BusinessRuleException("EVENT_ENDED", "This event has already ended");
        Member m = members.save(new Member(users.getReferenceById(family.id()), event, req.fullName().trim(), req.age(),
                req.gender(), req.notes()));
        audit.log(family.id(), "MEMBER_REGISTERED", "MEMBER", m.getId(), null);
        return MemberResponse.from(m);
    }

    @Transactional(readOnly = true)
    public List<MemberResponse> mine(AppUserPrincipal family) {
        return members.findForGuardian(family.id()).stream().map(MemberResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public MemberResponse get(Long id, AppUserPrincipal family) {
        return MemberResponse.from(owned(id, family));
    }

    /** PNG of the QR payload "MILANSETU:&lt;uuid&gt;". Owner only. */
    @Transactional(readOnly = true)
    public byte[] qrPng(Long id, AppUserPrincipal family) {
        return qr.png(QrTokens.payload(owned(id, family).getQrToken()), 400);
    }

    /** Printable wristband strip: QR, first name, event, instruction. No phone number, no surname, no notes. */
    @Transactional(readOnly = true)
    public String wristbandHtml(Long id, AppUserPrincipal family) {
        Member m = owned(id, family);
        String img = Base64.getEncoder().encodeToString(qr.png(QrTokens.payload(m.getQrToken()), 300));
        return """
               <!doctype html><html><head><meta charset="utf-8"><title>MilanSetu wristband</title>
               <style>body{font-family:system-ui,sans-serif;margin:24px}.band{display:flex;align-items:center;gap:16px;
               border:2px dashed #1f4f86;border-radius:12px;padding:12px 16px;width:620px}.band img{width:120px;height:120px}
               h1{margin:0;font-size:22px;color:#0f2a4a}p{margin:4px 0;font-size:13px}@media print{button{display:none}}</style></head>
               <body><div class="band"><img alt="QR code" src="data:image/png;base64,%s">
               <div><h1>%s</h1><p><strong>%s</strong></p>
               <p>If I am lost, please take me to the nearest <strong>help desk</strong> or police booth.</p>
               <p>Staff: scan this code with MilanSetu.</p></div></div>
               <p><button onclick="window.print()">Print wristband</button></p></body></html>
               """.formatted(img, HtmlUtils.htmlEscape(m.firstName()), HtmlUtils.htmlEscape(m.getEvent().getName()));
    }

    /** Lost wristband: issue a new token; the old one stops working immediately. */
    @Transactional
    public MemberResponse regenerate(Long id, AppUserPrincipal family) {
        Member m = owned(id, family);
        m.regenerateToken();
        audit.log(family.id(), "QR_REGENERATED", "MEMBER", id, null);
        return MemberResponse.from(m);
    }

    private Member owned(Long id, AppUserPrincipal family) {
        Member m = members.findWithDetails(id).orElseThrow(() -> new ResourceNotFoundException("Member", id));
        access.assertOwnsMember(family, m);
        return m;
    }
}
