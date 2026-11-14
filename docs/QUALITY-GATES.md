# Quality Gates

Product states:
1. **Specified** — valid manifest, target users, and core job are explicit.
2. **Implemented** — required journeys exist; no placeholder or mocked core flow.
3. **Built** — artifact exists; package identity and version are verified.
4. **Tested** — declared tests pass and evidence is retained.
5. **Security-reviewed** — permissions, data, network behavior, dependencies, and secrets are reviewed.
6. **Release candidate** — source revision, artifact checksum, release notes, and limitations are recorded.
7. **Released** — explicit approval and distribution record exist.

A failed mandatory gate blocks promotion. Missing evidence is not a pass. Compile-only can reach Built, never Tested or Release Candidate.
