import { api } from './client'

/** Images/HTML behind authentication can't be loaded with a plain <img src>, so fetch them as blobs. */
export async function fetchBlobUrl(url: string): Promise<string> {
  const res = await api.get(url, { responseType: 'blob' })
  return URL.createObjectURL(res.data as Blob)
}
