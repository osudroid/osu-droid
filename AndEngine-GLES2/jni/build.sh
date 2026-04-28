#!/bin/bash
set -e

SCRIPT_DIRECTORY="$(cd "$(dirname "$0")" && pwd)"
NDK_DIRECTORY="${ANDROID_NDK_HOME:-${ANDROID_NDK_ROOT:-}}"
PROJECT_DIRECTORY="$(cd "${SCRIPT_DIRECTORY}/.." && pwd)"

if [ -z "${NDK_DIRECTORY}" ]; then
	echo "ANDROID_NDK_HOME (or ANDROID_NDK_ROOT) is not set."
	exit 1
fi

# Run build:
pushd "${PROJECT_DIRECTORY}"
"${NDK_DIRECTORY}/ndk-build"

# Clean temporary files:
# rm -rf "${PROJECT_DIRECTORY}/obj"
# find . -name gdbserver -print0 | xargs -0 rm -rf
# find . -name gdb.setup -print0 | xargs -0 rm -rf

popd