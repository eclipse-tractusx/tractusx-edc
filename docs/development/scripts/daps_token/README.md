# DAPS Token Script

Script to request an IDS token from the DAPS.

## Usage

1. Copy your DAPS private key into `key.pem`
2. Edit in the script the following variables
   - `token_url`
   - `client_id`
   - `resource`
3. Run script

   ```bash
   ./daps_auth_sh
   ```

4. Take the `access_token` from the output in use it in IDS messages. The output of the script looks like this:

   ```json
   {
     "access_token": "eyJ0eXAiOiJhdCtqd3QiLCJraWQiOiI3MDM2MzAwNzVkYTM2N2IxYmZiYjRjY2Q0N2M1Y2ViMGQ5ZjM1MmRmYWU2MzJkMzYxMGMxNzNmMTM1NDI0NmM5IiwiYWxnIjoiUlMyNTYifQ.eyJzY29wZSI6Imlkc2M6SURTX0NPTk5FQ1RPUl9BVFRSSUJVVEVTX0FMTCIsImF1ZCI6WyJodHRwczovL3Blbi10ZXN0LXBsYXRvLXR4ZGMuaW50LmRlbW8uY2F0ZW5hLXgubmV0L2FwaS92MS9pZHMvZGF0YSJdLCJpc3MiOiJodHRwOi8vaWRzLWRhcHM6NDU2Ny8iLCJzdWIiOiI5OTo4MzpBNzoxNzo4NjpGRjo5ODo5MzpDRTpBMDpERDpBMTpGMTozNjpGQTpGNjowRjo3NTowQToyMzprZXlpZDo5OTo4MzpBNzoxNzo4NjpGRjo5ODo5MzpDRTpBMDpERDpBMTpGMTozNjpGQTpGNjowRjo3NTowQToyMyIsIm5iZiI6MTY3ODMxMDE0OSwiaWF0IjoxNjc4MzEwMTQ5LCJqdGkiOiJkZmY5Y2FmOS05NDZiLTQ1YmMtOWY4My0yYmJkMDI4NTlmYWMiLCJleHAiOjE2NzgzMTM3NDksImNsaWVudF9pZCI6Ijk5OjgzOkE3OjE3Ojg2OkZGOjk4OjkzOkNFOkEwOkREOkExOkYxOjM2OkZBOkY2OjBGOjc1OjBBOjIzOmtleWlkOjk5OjgzOkE3OjE3Ojg2OkZGOjk4OjkzOkNFOkEwOkREOkExOkYxOjM2OkZBOkY2OjBGOjc1OjBBOjIzIiwicmVmZXJyaW5nQ29ubmVjdG9yIjoiaHR0cDovL3BsYXRvLWNvbnRyb2xwbGFuZS9CUE5QTEFUTyJ9.JQqt9gCpaG7rLztO5-pJa7HIybVjKog9v0CFXHoVJZgdxMc5nTKZnuwBVHC1PXuWrBiyPxPoNg0TsfRg9DqF8rFD5noarxOJ1S84BF7AUUi3phQzBF26lsmNmOW_gdNBC-8xw1WMo5hRHH56cB64_x4V8T4VwFlSYYrmA5ge_EiPCW_KWF9sNguXBKs8uTbLB3lvTELGTjmZI93tVR-vYuYzW2jxH1PJNW29KJRQcM0D1AiveMs3_ThRjheEvugyh9QIY1RwPXMgYQpSTvoumNuFFTnpR21ueWfSUtU-4Qu9suNTkcaFihvEObXVrhyMja-HjhQaC8i0XsAgY0tT1A",
     "expires_in": 3600,
     "token_type": "bearer",
     "scope": "idsc:IDS_CONNECTOR_ATTRIBUTES_ALL"
   }
   ```
