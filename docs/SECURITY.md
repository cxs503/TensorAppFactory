# Security and Release Safety

- Never commit API keys, OAuth secrets, passwords, keystores, or signing material.
- CI debug signing is for verification only. Production signing keys belong in protected secret storage with restricted access and recovery procedures.
- Request only Android permissions required by a demonstrated feature.
- Declare personal-data categories, purpose, retention, network destinations, and privacy policy before release.
- Review and pin third-party actions and dependencies before production use.
- Generate dependency and license inventories before distribution.
- Treat generated code and dependencies as untrusted until reviewed and tested.
- Do not publish to an app store automatically without explicit release approval.
