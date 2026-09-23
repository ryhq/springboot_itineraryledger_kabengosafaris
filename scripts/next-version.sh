#!/usr/bin/env bash
#
# The next version number, worked out rather than typed.
#
# A version somebody edits by hand is a version somebody forgets to edit. It sat at 1.0.0 across
# several releases that changed real behaviour, which makes "which build are you on?" unanswerable
# precisely when it matters — during an incident, from a client's screenshot.
#
# The rule, in full:
#   * the last `vX.Y.Z` git tag is the current version; with no tags, the version in pom.xml is
#   * every release bumps the PATCH
#   * a commit since that tag whose message contains [minor] bumps the MINOR and resets the patch
#   * ... [major] bumps the MAJOR and resets both
#
# Markers are read from EVERY commit in the range, not just the newest, so a release that sweeps up
# several commits cannot lose the one that declared itself a feature.
#
# This only PRINTS. The tag is written by the workflow after a deploy has actually succeeded, so a
# failed build never consumes a number and two people never race for the same one.

set -euo pipefail
cd "$(dirname "$0")/.."

last_tag=$(git tag -l 'v[0-9]*.[0-9]*.[0-9]*' --sort=-v:refname | head -1 || true)

if [ -n "$last_tag" ]; then
  current=${last_tag#v}
  range="$last_tag..HEAD"
else
  # No tags yet: seed from the file that has been carrying the version by hand until now.
  #
  # The <parent> block is skipped first. Without that this reads Spring Boot's own version and
  # cheerfully proposes 3.5.8 as the next release of this application.
  current=$(awk '
    /<parent>/     {inparent=1}
    /<\/parent>/   {inparent=0; next}
    !inparent && /<version>/ && !done {
      gsub(/.*<version>|<\/version>.*/, ""); print; done=1
    }' pom.xml)
  current=${current:-0.0.0}
  range="HEAD"
fi

IFS=. read -r major minor patch <<< "${current%%-*}"
major=${major:-0}; minor=${minor:-0}; patch=${patch:-0}

messages=$(git log --format=%B "$range" 2>/dev/null || echo "")

if grep -qiF '[major]' <<< "$messages"; then
  major=$((major + 1)); minor=0; patch=0
elif grep -qiF '[minor]' <<< "$messages"; then
  minor=$((minor + 1)); patch=0
else
  patch=$((patch + 1))
fi

echo "${major}.${minor}.${patch}"
