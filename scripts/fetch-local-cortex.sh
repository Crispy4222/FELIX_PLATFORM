#!/usr/bin/env bash
set -Eeuo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
INCLUDES_DIR="$ROOT_DIR/iso/config/includes.chroot"
LIBEXEC_DIR="$INCLUDES_DIR/usr/local/libexec"
MODEL_DIR="$INCLUDES_DIR/opt/felix-models"
DOC_DIR="$INCLUDES_DIR/usr/share/doc/felix-platform"
CACHE_DIR="${FELIX_BUILD_CACHE:-$ROOT_DIR/.build-cache}"

BUNDLE_MODEL="${FELIX_BUNDLE_MODEL:-1}"
if [[ "$BUNDLE_MODEL" != "1" ]]; then
  echo "FELIX_BUNDLE_MODEL=$BUNDLE_MODEL; building control plane without resident model weights"
  exit 0
fi

LLAMA_TAG="b10199"
MODEL_REPO="Qwen/Qwen2.5-1.5B-Instruct-GGUF"
MODEL_REV="91cad51170dc346986eccefdc2dd33a9da36ead9"
MODEL_FILE="qwen2.5-1.5b-instruct-q4_k_m.gguf"
MODEL_URL="https://huggingface.co/${MODEL_REPO}/resolve/${MODEL_REV}/${MODEL_FILE}?download=true"
LICENSE_URL="https://huggingface.co/${MODEL_REPO}/resolve/${MODEL_REV}/LICENSE?download=true"

command -v git >/dev/null 2>&1 || { echo "error: git is required" >&2; exit 1; }
command -v cmake >/dev/null 2>&1 || { echo "error: cmake is required" >&2; exit 1; }
command -v curl >/dev/null 2>&1 || { echo "error: curl is required" >&2; exit 1; }

mkdir -p "$LIBEXEC_DIR" "$MODEL_DIR" "$DOC_DIR" "$CACHE_DIR"

LLAMA_SRC="$CACHE_DIR/llama.cpp-$LLAMA_TAG"
if [[ ! -x "$LIBEXEC_DIR/felix-llama-server" ]]; then
  rm -rf "$LLAMA_SRC"
  git clone --depth 1 --branch "$LLAMA_TAG" https://github.com/ggml-org/llama.cpp.git "$LLAMA_SRC"
  cmake -S "$LLAMA_SRC" -B "$LLAMA_SRC/build" \
    -DCMAKE_BUILD_TYPE=Release \
    -DBUILD_SHARED_LIBS=OFF \
    -DGGML_NATIVE=OFF \
    -DGGML_OPENMP=ON \
    -DLLAMA_CURL=OFF \
    -DLLAMA_BUILD_TESTS=OFF \
    -DLLAMA_BUILD_EXAMPLES=OFF
  cmake --build "$LLAMA_SRC/build" --config Release --target llama-server -j"$(nproc)"
  install -m 0755 "$LLAMA_SRC/build/bin/llama-server" "$LIBEXEC_DIR/felix-llama-server"
fi

if [[ ! -s "$MODEL_DIR/$MODEL_FILE" ]]; then
  curl --fail --location --retry 5 --retry-all-errors --continue-at - \
    --output "$MODEL_DIR/$MODEL_FILE.part" "$MODEL_URL"
  mv "$MODEL_DIR/$MODEL_FILE.part" "$MODEL_DIR/$MODEL_FILE"
fi

curl --fail --location --retry 5 --retry-all-errors \
  --output "$DOC_DIR/QWEN2.5-LICENSE" "$LICENSE_URL"

(
  cd "$MODEL_DIR"
  sha256sum "$MODEL_FILE" > "$DOC_DIR/MODEL_SHA256SUMS"
)

cat > "$DOC_DIR/MODEL_SOURCE.txt" <<EOF
Resident model: Qwen2.5-1.5B-Instruct-GGUF Q4_K_M
Repository: https://huggingface.co/$MODEL_REPO
Pinned revision: $MODEL_REV
File: $MODEL_FILE
Runtime: llama.cpp $LLAMA_TAG
Runtime repository: https://github.com/ggml-org/llama.cpp
Inference endpoint: http://127.0.0.1:8081/v1/chat/completions
EOF

printf 'Resident FELIX cortex ready: %s\n' "$MODEL_DIR/$MODEL_FILE"
