<script setup>
import DefaultTheme from 'vitepress/theme'

const { Layout } = DefaultTheme
</script>

<template>
  <Layout>
    <template #home-features-before>
      <section class="sig">
        <div class="sig-card">
          <span class="sig-label">CURRICULUM · 18 DEMOS</span>
          <h3 class="sig-title">从第一个对话到多模态应用</h3>
          <div class="st-ladder">
            <span class="st-step st-mil" style="--i:1">01<i>基础对话</i></span>
            <span class="st-step" style="--i:2">02</span>
            <span class="st-step" style="--i:3">03</span>
            <span class="st-step" style="--i:4">04</span>
            <span class="st-step st-mil" style="--i:5">05<i>工具调用</i></span>
            <span class="st-step" style="--i:6">06</span>
            <span class="st-step" style="--i:7">07</span>
            <span class="st-step st-mil" style="--i:8">08<i>RAG</i></span>
            <span class="st-step" style="--i:9">09</span>
            <span class="st-step" style="--i:10">10</span>
            <span class="st-step" style="--i:11">11</span>
            <span class="st-step st-mil" style="--i:12">12<i>Advisor</i></span>
            <span class="st-step" style="--i:13">13</span>
            <span class="st-step" style="--i:14">14</span>
            <span class="st-step" style="--i:15">15</span>
            <span class="st-step st-mil" style="--i:16">16<i>MCP</i></span>
            <span class="st-step st-mil" style="--i:17">17<i>多模态</i></span>
            <span class="st-step st-mil" style="--i:18">18<i>售后实战</i></span>
          </div>
          <p class="sig-note">18 个渐进式 Demo，每个都是可独立运行的模块：Chat、流式、工具调用、RAG、ETL 管道、Advisor、结构化输出、MCP、多模态、Agent Skills——爬完这架梯子，AI 应用开发的地图就在手里了。</p>
        </div>
      </section>
    </template>
    <template #home-features-after>
      <div class="sig-more"><a href="https://ibqy.github.io/" target="_blank" rel="noopener">← ibqy.github.io · 探索全部项目</a></div>
    </template>
  </Layout>
</template>
