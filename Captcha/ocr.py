#!/usr/bin/env python3
"""
验证码识别 CLI —— ddddocr 本地离线 OCR + pytesseract 兜底

用法：
    python3 ocr.py 图片.png                  # 识别一张
    python3 ocr.py logs/*.png                # 批量
    python3 ocr.py 图.png --ranges 0         # 限定字符集（0=纯数字，见下）
    python3 ocr.py 图.png --old              # 用旧模型 common_old.onnx
    python3 ocr.py 图.png --no-fallback      # 关掉 tesseract 兜底
    cat 图.png | python3 ocr.py -            # 从 stdin 读

--ranges 常用值：0=纯数字 1=纯小写 2=纯大写 3=小写+数字 4=大写+数字 5=大小写 6=大小写+数字
也可以直接传自定义字符集字符串，例如 --ranges "abcd1234"
"""
import argparse
import sys
from pathlib import Path

# 优先用随包携带的 vendor/ddddocr（模型也在里面），环境里没装也能跑
sys.path.insert(0, str(Path(__file__).parent / "vendor"))

import ddddocr  # noqa: E402

_DIGITS = "0123456789"
_LOWER = "abcdefghijklmnopqrstuvwxyz"
_UPPER = _LOWER.upper()

# ddddocr 1.6.1 的 set_ranges(整数预设) 有 bug，会返回空结果；
# 传自定义字符集字符串则正常。所以这里把预设编号自己翻译成字符集串。
_RANGE_PRESETS = {
    0: _DIGITS,
    1: _LOWER,
    2: _UPPER,
    3: _LOWER + _DIGITS,
    4: _UPPER + _DIGITS,
    5: _LOWER + _UPPER,
    6: _LOWER + _UPPER + _DIGITS,
}


def load_bytes(target: str) -> bytes:
    if target == "-":
        return sys.stdin.buffer.read()
    return Path(target).read_bytes()


def tesseract_fallback(img_bytes: bytes) -> str:
    """ddddocr 出空结果时的兜底。装了 tesseract 才有效。"""
    try:
        import io

        import pytesseract
        from PIL import Image

        img = Image.open(io.BytesIO(img_bytes)).convert("L")
        return pytesseract.image_to_string(
            img, config="--psm 7 --oem 3"
        ).strip()
    except Exception:
        return ""


def main() -> int:
    ap = argparse.ArgumentParser(description="验证码识别（ddddocr 离线）")
    ap.add_argument("images", nargs="+", help="图片路径，或 - 表示从 stdin 读")
    ap.add_argument("--ranges", help="限定字符集，见文件头说明")
    ap.add_argument("--old", action="store_true", help="用旧模型 common_old.onnx")
    ap.add_argument("--no-fallback", action="store_true", help="不使用 tesseract 兜底")
    ap.add_argument("--quiet", action="store_true", help="只输出识别结果，不带文件名")
    args = ap.parse_args()

    ocr = ddddocr.DdddOcr(show_ad=False, old=args.old)
    if args.ranges is not None:
        charset = args.ranges
        if charset.isdigit() and len(charset) == 1:
            charset = _RANGE_PRESETS.get(int(charset), charset)
        ocr.set_ranges(charset)

    exit_code = 0
    for target in args.images:
        try:
            img_bytes = load_bytes(target)
        except Exception as e:
            print(f"{target}\t<读取失败: {e}>", file=sys.stderr)
            exit_code = 1
            continue

        try:
            text = (ocr.classification(img_bytes) or "").strip()
        except Exception as e:
            print(f"{target}\t<识别失败: {e}>", file=sys.stderr)
            exit_code = 1
            continue

        source = "ddddocr"
        if not text and not args.no_fallback:
            text = tesseract_fallback(img_bytes)
            source = "tesseract" if text else "空"

        if args.quiet:
            print(text)
        else:
            print(f"{target}\t{text}\t[{source}]")

    return exit_code


if __name__ == "__main__":
    raise SystemExit(main())
