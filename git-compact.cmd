"C:\Program Files\Git\bin\sh.exe" -login -i -c "git gc --prune=now --aggressive"
"C:\Program Files\Git\bin\sh.exe" -login -i -c "git reflog expire --all"
"C:\Program Files\Git\bin\sh.exe" -login -i -c "git clean -xdf"
pause