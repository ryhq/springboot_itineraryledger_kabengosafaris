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
#   * a commit since that tag whose SUBJECT LINE ends with [minor] bumps the MINOR, patch to 0
#   * ... [major] bumps the MAJOR, minor and patch to 0
#
# Subject lines only, and only at the END of one. The first version of this read the whole message
# body, and the very first release proved why that is wrong: the commit that INTRODUCED this script
# explained the rule in its body, the script read its own documentation as an instruction, and
# tagged v2.0.0 for a change that broke nothing. A marker has to be somewhere prose never goes.
#
# Every commit in the range is checked, not just the newest, so a release sweeping up several
# commits cannot lose the one that declared itself a feature.
#
# This only PRINTS. The tag is written by the workflow after a deploy has actually succeeded, so a
# failed build never consumes a number and two people never race for the same one.

set -euo pipefail
cd "$(dirname "$0")/.."

last_tag=$(git tag -l 'v[0-9]*.[0-9]*.[0-9]*' --sort=-v:refname | head -1 || true)

if [ -n "$last_tag" ]; then
  current=${last_tag#v}
  range="$last_tag..HEAD"
  subjects=$(git log --format=%s "$range" 2>/dev/null || echo "")
else
  # No tag yet, so "since the last release" covers nothing: there has not been one under this
  # scheme. Scanning the whole history instead would let any commit ever written decide the first
  # automatic version, which is exactly how the first run produced a major bump.
  subjects=""
  range=""
  # No tag yet: seed from the <revision> property, which is the fallback the pom carries for a
  # local build. Not <version> — that is literally "${revision}" now, and reading it produced a
  # first release called ${revision}.0.1.
  current=$(sed -n 's|.*<revision>\(.*\)</revision>.*|\1|p' pom.xml | head -1)
  current=${current:-0.0.0}
  range="HEAD"
fi

IFS=. read -r major minor patch <<< "${current%%-*}"
major=${major:-0}; minor=${minor:-0}; patch=${patch:-0}

if grep -qiE '\[major\][[:space:]]*$' <<< "$subjects"; then
  major=$((major + 1)); minor=0; patch=0
elif grep -qiE '\[minor\][[:space:]]*$' <<< "$subjects"; then
  minor=$((minor + 1)); patch=0
else
  patch=$((patch + 1))
fi

# A version that is not three plain numbers is a version that got here by accident — an unexpanded
# ${revision}, an empty seed, a stray letter. It has already happened once: reading the pom's
# <version> gave the literal "${revision}" and the API went out reporting ".0.0", which is not a
# version anybody can quote back. Refuse rather than tag it.
result="${major}.${minor}.${patch}"
if ! [[ "$result" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
  echo "next-version: refusing to emit \"$result\" — seeded from \"$current\"" >&2
  exit 1
fi

echo "$result"
