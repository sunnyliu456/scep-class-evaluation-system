# SCEP Full Project

面向班级综合测评的批量计算系统，支持多源 Excel/ZIP 数据导入、自动评分合并、可视化分析与一键导出。

## 项目特性

- 6 步向导式流程：智育、奖励、体育、德育、劳育、汇总导出。
- 支持大文件分片上传：断点续传、并发上传、服务端合并。
- 多源数据自动匹配：按学号聚合不同维度成绩并计算总分。
- 汇总分析可视化：柱状图、雷达图、统计卡片。
- Excel 导出：生成班级综测汇总结果。

## 技术栈

### 前端

- React 18 + TypeScript
- Vite 5
- Ant Design 5
- Axios
- ECharts 6（按需引入）

### 后端

- Spring Boot 2.7
- EasyExcel
- Apache Commons Lang

## 目录结构

```text
backend/   Spring Boot 后端
frontend/  React 前端
```

## 快速开始

### 1) 启动后端

```bash
cd backend
mvn spring-boot:run
```

默认端口：`8080`

### 2) 启动前端

```bash
cd frontend
npm install
npm run dev
```

默认地址：`http://localhost:5173`

前端已配置 `/api` 代理到 `http://localhost:8080`。

## 生产构建

### 前端

```bash
cd frontend
npm run build
```

### 后端

```bash
cd backend
mvn -DskipTests package
```

## 关键接口

- `POST /api/import/gradesZip`：上传智育 ZIP
- `POST /api/import/reward`：上传奖励 ZIP/Excel
- `POST /api/import/pe`：上传体育数据
- `POST /api/import/moral`：上传德育数据
- `POST /api/import/dorm`：上传劳育双表
- `GET /api/summary`：获取当前汇总
- `GET /api/summary/export`：导出汇总 Excel

### 分片上传相关

- `POST /api/upload/chunk/init`
- `POST /api/upload/chunk/part`
- `POST /api/upload/chunk/status`
- `POST /api/import/gradesZip/chunk/complete`
- `POST /api/import/reward/chunk/complete`

## 性能优化亮点

- 步骤级懒加载（Step0-Step5）
- ECharts 模块化按需加载（`echarts/core` + charts/components）
- `manualChunks` 拆分 vendor（react/antd/echarts），提升缓存复用

## 开源说明

欢迎提交 Issue 和 PR。建议在 PR 中附带复现步骤与截图，便于快速评审。

- 贡献指南：`CONTRIBUTING.md`
- 开源协议：`LICENSE`（MIT）
