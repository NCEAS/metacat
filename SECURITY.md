# Security Policy

## Supported Versions

Metacat follows a latest-release support model.

- Only the most recent product release is supported for security fixes.
- Security fixes are not backported to older releases.
- Users should upgrade to the latest release to receive security updates.

| Version | Supported |
| --- | --- |
| Latest release (tip of `main` branch) | :white_check_mark: |
| All previous releases | :x: |

This policy aligns with the project release workflow documented in [CONTRIBUTING.md](CONTRIBUTING.md), where the `main` branch tip reflects the most recent release.

## Reporting a Vulnerability

Please **do not report security vulnerabilities in public GitHub issues or pull requests**.

Instead, use GitHub's private vulnerability reporting workflow:

1. Go to the [Metacat Security Advisories](https://github.com/NCEAS/metacat/security) page.
2. Click **Report a vulnerability**.
3. Provide details to help us reproduce and assess the issue:
   - affected version(s)
   - deployment context and configuration
   - impact description
   - clear reproduction steps or proof of concept
   - suggested remediation (if available)

Direct link (while logged in to GitHub):

- https://github.com/NCEAS/metacat/security/advisories/new

If you are unable to use GitHub private reporting, contact the maintainers at:

- sysadmin@dataone.org

Use a subject line starting with `SECURITY:` and include the same details listed above.

## What to Expect After You Report

We aim to follow these targets (best effort):

- Initial acknowledgment: within 3 business days
- Triage decision (validity/severity/scope): within 10 business days
- Ongoing status updates: at least every 10 business days while actively investigating

If a report is accepted, maintainers will:

- validate and prioritize the issue
- develop and test a fix in the normal release workflow
- publish a fix in the next available release
- publish a security advisory when appropriate

Because only the newest release is supported, accepted fixes are released only in the latest product release.

## Coordinated Disclosure

Please allow maintainers a reasonable amount of time to investigate, patch, and publish guidance before public disclosure.

The project will coordinate disclosure timing with reporters whenever possible. After a fix is available, maintainers may publish details through a GitHub Security Advisory and release notes.

## Security Update Guidance for Users

To reduce risk:

- run the latest Metacat release
- subscribe to repository release notifications
- monitor published release notes and security advisories

For release details, see [README.md](README.md) and [RELEASE-NOTES.md](RELEASE-NOTES.md).

## Scope

This policy applies to vulnerabilities in source code and officially maintained release artifacts in this repository.

Out-of-scope examples may include:

- vulnerabilities only affecting unsupported versions
- issues requiring unrealistic attack preconditions
- reports consisting only of automated scanner output without a reproducible impact

Even if an issue is out of scope for a formal security response, maintainers may still accept quality bug reports through normal channels.
