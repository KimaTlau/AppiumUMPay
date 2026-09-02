# Captcha 识别工具（ddddocr 离线打包版）

从 `~/Downloads/admin-automation/` 那套 welive / 1083int 自动化里抽出来的验证码识别能力，
打包成独立可用的工具。**模型随包携带，断网也能跑。**

## 目录

```
Captcha识别工具/
├── ocr.py                  # 命令行工具（主入口）
├── vendor/ddddocr/         # ddddocr 1.6.1 完整包 + 模型（85MB）
│   ├── common.onnx         #   主模型 54MB
│   ├── common_det.onnx     #   目标检测 20MB
│   └── common_old.onnx     #   旧模型 13MB
├── example_integration.py  # 嵌到自己代码里的参考写法（原项目同款）
└── sample_captcha*.png     # 真实验证码样本，可拿来自测
```

## 用法

```bash
python3 ocr.py 图片.png                  # 识别一张
python3 ocr.py logs/*.png                # 批量
python3 ocr.py 图.png --ranges 0         # 限定纯数字，准确率更高
python3 ocr.py 图.png --quiet            # 只输出结果，方便管道
python3 ocr.py 图.png --old              # 换旧模型（新模型认不准时试试）
cat 图.png | python3 ocr.py -            # 从 stdin 读
```

输出格式：`文件名 <TAB> 识别结果 <TAB> [来源]`

`--ranges` 常用值：`0`=纯数字 `1`=纯小写 `2`=纯大写 `3`=小写+数字 `4`=大写+数字
`5`=大小写 `6`=大小写+数字。也可直接传自定义字符集，如 `--ranges "abcd1234"`。

**知道验证码是纯数字/纯字母时，加 `--ranges` 能减少认错。**

> ⚠️ 打包时踩到的坑：ddddocr 1.6.1 自己的 `set_ranges(整数预设)` 是**坏的**，
> 传 `0` 会直接返回空结果。`ocr.py` 里已经把预设编号翻译成字符集字符串绕过了，
> 但如果你直接调用 ddddocr（不经过本工具），记得传字符串 `"0123456789"` 而不是 `0`。

## 依赖

已在本机确认齐全（Homebrew Python 3.14）：

| 组件 | 版本 | 用途 |
|---|---|---|
| onnxruntime | 1.25.1 | 跑 onnx 模型，必需 |
| Pillow | 12.2.0 | 图片处理，必需 |
| numpy | 2.4.1 | 必需 |
| pytesseract + tesseract | 已装 | 兜底，可选 |

ddddocr 本身不用装 —— `ocr.py` 会优先加载 `vendor/ddddocr`。
换机器时把整个文件夹拷过去，只需补 `pip3 install onnxruntime pillow numpy`。

## 实测

```
sample_captcha.png   → 6179   ✅ 肉眼核对一致
sample_captcha2.png  → 6964   ⚠️ 图本身第二位 8/9 难辨，未能确认
```

带干扰线的手写体验证码本来就不是 100%，**生产用法要配重试**：识别→提交→失败就换一张再识别。
原项目 `get_creds_parallel.py` 就是这么干的。

## 另一条路：Claude API 识图

`~/Downloads/admin-automation/test_captcha.py` 是用 Claude 视觉识别验证码的版本
（Playwright 截图 → 丢给 Claude）。慢、要联网、要花钱，但遇上 ddddocr 啃不动的
复杂验证码时可以顶上。这里没打包进来，需要时去原目录拿。
