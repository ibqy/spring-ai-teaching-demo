#!/bin/bash
# 教学示例脚本：统计知识库文档信息（供 script-runner 技能调用）
# 作者：ibqy | 日期：2026-09-14
echo "===== 小北优选咖啡店 · 知识库文档统计 ====="
for f in src/main/resources/kb/*.md src/main/resources/kb/*.txt; do
  if [ -f "$f" ]; then
    lines=$(wc -l < "$f")
    echo "文档：$(basename "$f") —— $lines 行"
  fi
done
echo "===== 统计完成 ====="
