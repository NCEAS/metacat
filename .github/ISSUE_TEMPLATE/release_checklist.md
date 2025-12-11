---
name: Release Checklist
about: A release-prep checklist
title: "Release Checklist"
labels:
  - release
assignees: []
hidden: true    # do NOT show in template picker every time someone creates a new issue
---

## Release checklist

> [!TIP]
> Create an issue from this template [using this link](https://github.com/NCEAS/metacat/issues/new?template=release_checklist.md)

### CHOOSE ONE, AND DELETE THE OTHER:

---
### EITHER:

### Release of Software & helm Chart Together

- [ ] Create a branch named `feature-<issueNum>-<releaseVer>-release-prep`, and do the following:
  - [ ] **pom.xml**: Update `<version>x.x.x</version>`
  - [ ] **Chart.yaml**: Update chart `version` and `appVersion`
  - [ ] Grep codebase for previous release number, just in case
  - [ ] **RELEASE-NOTES.md**:
    - [ ] Update for new app & chart versions
    - [ ] DON'T FORGET TO SET CORRECT RELEASE DATE!
  - [ ] `pom.xml`: Update `<metacat_common_version>x.x.x</metacat_common_version>`
  - [ ] `metacat-common/pom.xml`: Update `<version>x.x.x</version>`
  - [ ] `metacat-index/pom.xml`: Update `<version>x.x.x</version>` and `<metacat_common_version>x.x.x</metacat_common_version>`
  - `build.properties`:
    - [ ] update `metacat.version` and `metacatui.tag`
  - `SQL`:
    - [ ] Add a new db upgrade script for this version
    - [ ] Modify the version in `loaddtdschema-postgres.sql`
  - `README.md`:
    - [ ] Update release number
    - [ ] Add new DOI for this release (2 places)
  - `metacat.properties`:
    - [ ] Update the version
    - [ ] Add a new db upgrade script property
  - [ ] Draft release email
  - [ ] PR & merge release prep branch to `develop`
- [ ] PR & merge `develop` -> `main`
- [ ] **(Linux VM)** build and push docker image
- [ ] **(Mac)** package and push helm chart
- [ ] **(Mac)** build binary packages & upload to knb site
- [ ] Tag the release; look up the `<commit-sha>` from `git log`, then:
  ```shell
  git tag x.x.x <commit-sha>
  git tag chart-x.x.x <commit-sha>
  git push --tags    ## IMPORTANT - DON'T FORGET THIS!
  ```
- [ ] Add the metadata for the reserved DOI and publish it with the correct softwareheritage url
- [ ] Add to GH `Releases` page (**& link to binaries that are on KNB site - don't upload to GH!**)
- [ ] Send email
- [ ] announce on Slack

---
### OR:

### Release of helm Chart Only

- [ ] `git checkout <TAG NUMBER OF EXISTING RELEASE>`
- [ ] create branch from this tag: `feature-<issueNum>-<releaseVer>-release-prep`
  - [ ] Chart.yaml: Update chart version and any other details necessary
  - [ ] grep for previous release number, just in case
  - [ ] RELEASE-NOTES.md:
    - [ ] Update for new version(s).
    - [ ] DON'T FORGET TO SET CORRECT RELEASE DATE!
  - [ ] `git cherry-pick` any commits that need to be included from develop
  - [ ] PR & merge to develop
  - [ ] PR & merge to main
- [ ] **(Mac)** package and push helm chart
  ```shell
  helm package -u ./helm
  
  helm push metacat-<x.x.x>.tgz oci://ghcr.io/nceas/charts
  ```
- [ ] Tag the release; look up the `<commit-sha>` from `git log`, then:
  ```shell
  git tag chart-x.x.x <commit-sha>
  git push --tags    ## IMPORTANT - DON'T FORGET THIS!
  ```
- [ ] Add to GH `Releases` page
- [ ] Announce on Slack?
