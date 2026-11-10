"""重建 App 内置的台标文件名清单。

上游 fanmingming/live 的 tv/ 目录增删文件后，跑一次本脚本即可：
    python tools/gen_logo_names.py

产物 app/src/main/assets/logo-names.txt 是纯文件名（不含扩展名、一行一个），
App 用它做本地命中判定，命中才去 CDN 拉图。
"""
import pathlib
import re
import urllib.request

URL = "https://github.com/fanmingming/live/tree/main/tv"
OUT = pathlib.Path(__file__).resolve().parent.parent / "app/src/main/assets/logo-names.txt"
MIN_FILES = 500


def main():
    request = urllib.request.Request(URL, headers={"User-Agent": "Mozilla/5.0"})
    html = urllib.request.urlopen(request, timeout=30).read().decode("utf-8", "ignore")
    files = sorted({m.group(1)[:-4] for m in re.finditer(r'"name":"([^"]+\.png)"', html)})
    if len(files) < MIN_FILES:
        raise SystemExit(f"目录解析异常，仅 {len(files)} 个文件，未写入")
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text("\n".join(files) + "\n", encoding="utf-8")
    print(f"{len(files)} files -> {OUT}")


if __name__ == "__main__":
    main()
