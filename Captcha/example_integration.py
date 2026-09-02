#!/usr/bin/env python3
"""
嵌到自己项目里的参考写法 —— 跟原项目 get_creds_parallel.py:143-152 同款结构，
区别是这里优先用随包携带的 vendor/ddddocr，环境里没装 ddddocr 也能跑。

跑一下看效果：
    python3 example_integration.py sample_captcha.png
"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent / "vendor"))

# ---- 初始化（原项目同款的 try/except 降级结构）----
try:
    import ddddocr as _ddddocr
    import pytesseract as _pytesseract
    from PIL import Image as _PILImage

    _ocr = _ddddocr.DdddOcr(show_ad=False)
    _LOCAL_OCR = True
except Exception:
    _LOCAL_OCR = False
    _ocr = _PILImage = _pytesseract = None


DIGITS = "0123456789"


def recognize(image_path: str, ranges: str | None = None) -> str:
    """
    识别验证码。ranges 传字符集字符串限定输出，例如 DIGITS 表示只可能是数字。

    ⚠️ ranges 必须传字符集【字符串】，不能传 ddddocr 文档里的整数预设 ——
       1.6.1 的整数预设是坏的，set_ranges(0) 会让识别结果直接变空。

    识别不出来返回空字符串 —— 调用方应当据此重新拉一张验证码重试。
    """
    if not _LOCAL_OCR:
        return ""

    if ranges is not None:
        _ocr.set_ranges(ranges)

    img_bytes = Path(image_path).read_bytes()

    try:
        text = (_ocr.classification(img_bytes) or "").strip()
    except Exception:
        text = ""

    # ddddocr 认不出来时用 tesseract 兜一手
    if not text and _pytesseract is not None:
        try:
            import io

            img = _PILImage.open(io.BytesIO(img_bytes)).convert("L")
            text = _pytesseract.image_to_string(img, config="--psm 7 --oem 3").strip()
        except Exception:
            text = ""

    return text


# ---- 生产用法：识别 + 重试 ----
def login_with_retry(fetch_captcha, submit, max_attempts: int = 5):
    """
    fetch_captcha() -> 验证码图片路径（每次调用应拉一张新的）
    submit(code)    -> 提交，成功返回真值
    验证码识别本来就不是 100%，靠重试把成功率堆上去。
    """
    for attempt in range(1, max_attempts + 1):
        code = recognize(fetch_captcha(), ranges=DIGITS)
        if not code:
            continue
        result = submit(code)
        if result:
            return result
        print(f"第 {attempt} 次识别 {code} 提交失败，换一张重试")
    return None


if __name__ == "__main__":
    if len(sys.argv) < 2:
        print(__doc__)
        raise SystemExit(1)

    print(f"本地 OCR 可用: {_LOCAL_OCR}")
    for path in sys.argv[1:]:
        print(f"{path} -> {recognize(path)!r}")
