@echo off
REM 把 docs/logo.svg 用 Edge headless 渲染成 5 个密度的 PNG。
REM Edge 完整支持 SVG（含 linearGradient），输出 PNG 一定能看到完整渐变 logo。

set EDGE="C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe"
set SVG=E:\Projects\PhotoBox\docs\logo.svg

REM mdpi 108 / hdpi 162 / xhdpi 216 / xxhdpi 324 / xxxhdpi 432
for %%S in (108 162 216 324 432) do (
    REM 用 data URI 在 HTML 里嵌入 svg，再 headless 截图指定尺寸
    setlocal enabledelayedexpansion
    set HTML=E:\Projects\PhotoBox\scripts\_logo_%%S.html
    (
        echo ^<html^>^<head^>^<style^>body{margin:0;padding:0;background:transparent}^</style^>^</head^>
        echo ^<body^>^<img src="file:///E:/Projects/PhotoBox/docs/logo.svg" width="%%S" height="%%S" /^>^</body^>^</html^>
    ) > !HTML!
    %EDGE% --headless --disable-gpu --no-sandbox --hide-scrollbars --window-size=%%S,%%S --screenshot=E:\Projects\PhotoBox\app\src\main\res\drawable\_tmp_logo_%%S.png file:///!HTML!
    del !HTML!
    endlocal
)
echo done