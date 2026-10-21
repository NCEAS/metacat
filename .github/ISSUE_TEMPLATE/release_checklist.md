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

- [ ] Create a branch named `task-<issueNum>-release-<releaseVer>-prep`, and do the following:
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
- [ ] Merge `main` back to `develop`
  ```shell
  git checkout develop; git merge main --ff-only
  git push
  ```
- [ ] **(Linux VM)** build and push docker image
- [ ] **(Mac)** package and push helm chart
- [ ] **(Mac)** build binary packages & upload to knb site
- [ ] Tag the release; look up the `<commit-sha>` from `git log`, then:
  ```shell
  # Always use annotated tags (-a) for releases
  git tag -a x.x.x <commit-sha> -m "Metacat Release x.x.x"
  git tag -a chart-x.x.x <commit-sha> -m "Metacat Helm Chart x.x.x"
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
- Create branch from this tag, that will be merged to `develop`:
- [ ] `git checkout -b task-<issueNum>-release-<releaseVer>-prep`
  - [ ] Chart.yaml: Update chart version and any other details necessary
  - [ ] grep for previous release number, just in case
  - [ ] RELEASE-NOTES.md:
    - [ ] Update for new version(s).
    - [ ] DON'T FORGET TO SET CORRECT RELEASE DATE!
  - [ ] `git cherry-pick` any commits that need to be included from develop
  - [ ] Create a PR to merge the branch to `develop`. If there are no conflicts, merge the PR
> [!IMPORTANT]
> - If there are conflicts:
>   ```shell
>   # first make a copy of the branch, to be used later, for the merge to main:
>   git checkout -b task-<issueNum>-release-<releaseVer>-main
>
>   # then switch back to the original branch and merge develop into it
>   git checkout -
>   git merge --no-commit --no-ff origin/develop
>   ```
>   ...and finally, resolve the conflicts, commit, push, and merge the PR.

- Create a PR to merge the release prep branch to `main`:
  - [ ] If there were NO CONFLICTS merging to `develop`, create a PR to merge `task-<issueNum>-release-<releaseVer>-prep` to `main`
  - [ ] If there WERE CONFLICTS merging to `develop`, create a PR to merge the new branch `task-<issueNum>-release-<releaseVer>-main` to `main`
    - If there are conflicts with this new PR, do a `git merge --no-commit --no-ff origin/main` into this branch and resolve them, then commit & push
  - [ ] When no conflicts remain, merge the PR to `main`

- [ ] Merge `main` back to `develop`
  ```shell
  git checkout develop; git merge main --ff-only
  git push
  ```

> [!TIP]
> if you've had to resolve conflicts, you'll likely see a message like this:
> ```
> $ git merge main --ff-only
> hint: Diverging branches can't be fast-forwarded, you need to either:
> hint: 	git merge --no-ff
> hint: or:
> hint: 	git rebase
> hint: Disable this message with "git config set advice.diverging false"
> fatal: Not possible to fast-forward, aborting.
> ```
> 
> If so, do a `git merge --no-ff` instead

- [ ] **(Mac)** package and push helm chart
  ```shell
  helm package -u ./helm
  
  helm push metacat-<x.x.x>.tgz oci://ghcr.io/nceas/charts
  ```
- [ ] Tag the release; look up the `<commit-sha>` from `git log`, then:
  ```shell
  # Always use annotated tags (-a) for releases
  git tag -a chart-x.x.x <commit-sha> -m "Metacat Helm Chart x.x.x"
  git push --tags    ## IMPORTANT - DON'T FORGET THIS!
  ```
- [ ] Add to GH `Releases` page
- [ ] Announce on Slack?
