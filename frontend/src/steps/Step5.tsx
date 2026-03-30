import { useEffect, useRef } from 'react'
import { Alert, Button, Card, Col, Row, Space } from 'antd'
import { BarChartOutlined, TrophyOutlined, UserOutlined, WarningOutlined } from '@ant-design/icons'
import type { EChartsType } from 'echarts/core'
import { BarChart, RadarChart } from 'echarts/charts'
import { GridComponent, RadarComponent, TitleComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { init, use } from 'echarts/core'
import type { StudentSummary, ScoreStats } from '../types'
import { baseColumns } from './columns'
import SimpleTable from '../components/SimpleTable'

use([TitleComponent, TooltipComponent, GridComponent, RadarComponent, BarChart, RadarChart, CanvasRenderer])

interface Props {
  className: string
  students: StudentSummary[]
  stats: ScoreStats | null
  onPrev: () => void
  onExportExcel: () => void
}

const Step5 = ({ className, students, stats, onPrev, onExportExcel }: Props) => {
  const chartContainer = useRef<HTMLDivElement | null>(null)
  const radarContainer = useRef<HTMLDivElement | null>(null)

  useEffect(() => {
    if (!chartContainer.current || !radarContainer.current || students.length === 0) {
      return
    }

    let disposed = false
    let barChart: EChartsType | null = null
    let radarChart: EChartsType | null = null
    let onResize: (() => void) | null = null

    const renderCharts = () => {
      if (disposed || !chartContainer.current || !radarContainer.current) {
        return
      }

      const distribution: Record<string, number> = {
        '90-105': 0,
        '80-89': 0,
        '70-79': 0,
        '60-69': 0,
        '<60': 0
      }

      students.forEach((item) => {
        if (item.total >= 90) distribution['90-105'] += 1
        else if (item.total >= 80) distribution['80-89'] += 1
        else if (item.total >= 70) distribution['70-79'] += 1
        else if (item.total >= 60) distribution['60-69'] += 1
        else distribution['<60'] += 1
      })

      barChart = init(chartContainer.current)
      barChart.setOption({
        title: { text: '综测总分分布', left: 'center' },
        tooltip: { trigger: 'axis' },
        xAxis: { type: 'category', data: Object.keys(distribution) },
        yAxis: { type: 'value', minInterval: 1 },
        series: [
          {
            type: 'bar',
            data: Object.values(distribution),
            itemStyle: { color: '#1677ff' },
            label: { show: true, position: 'top' }
          }
        ]
      })

      const len = students.length
      const average = {
        grade: students.reduce((acc, cur) => acc + cur.gradeScore, 0) / len,
        moral: students.reduce((acc, cur) => acc + cur.moralScore, 0) / len,
        sports: students.reduce((acc, cur) => acc + cur.sportsScore, 0) / len,
        reward: students.reduce((acc, cur) => acc + cur.rewardScore, 0) / len,
        labor: students.reduce((acc, cur) => acc + cur.laborScore, 0) / len
      }

      radarChart = init(radarContainer.current)
      radarChart.setOption({
        title: { text: '班级五育平均画像', left: 'center' },
        tooltip: {},
        radar: {
          indicator: [
            { name: '智育(70)', max: 70 },
            { name: '德育(15)', max: 15 },
            { name: '体育(5)', max: 5 },
            { name: '奖励(5)', max: 5 },
            { name: '劳育(5)', max: 5 }
          ]
        },
        series: [
          {
            name: '班级平均',
            type: 'radar',
            data: [
              {
                value: [
                  Number(average.grade.toFixed(2)),
                  Number(average.moral.toFixed(2)),
                  Number(average.sports.toFixed(2)),
                  Number(average.reward.toFixed(2)),
                  Number(average.labor.toFixed(2))
                ],
                name: '各项平均得分'
              }
            ],
            areaStyle: { opacity: 0.2, color: '#52c41a' },
            lineStyle: { color: '#52c41a' }
          }
        ]
      })

      onResize = () => {
        barChart?.resize()
        radarChart?.resize()
      }
      window.addEventListener('resize', onResize)
    }

    renderCharts()

    return () => {
      disposed = true
      if (onResize) {
        window.removeEventListener('resize', onResize)
      }
      barChart?.dispose()
      radarChart?.dispose()
    }
  }, [students])

  return (
    <div className="step-panel">
      <Alert
        message={`班级：${className}，共 ${students.length} 人；点击下方按钮导出 Excel（105 分制）`}
        type="success"
        showIcon
        className="mb-16"
      />

      {stats && (
        <div className="stats-dashboard mb-16">
          <Row gutter={[16, 16]}>
            <Col xs={24} sm={12} md={6}>
              <Card className="stat-card" bordered={false}>
                <div className="stat-row">
                  <div className="icon-box bg-blue">
                    <UserOutlined />
                  </div>
                  <div className="stat-info">
                    <div className="label">总人数</div>
                    <div className="value">{stats.count}</div>
                  </div>
                </div>
              </Card>
            </Col>
            <Col xs={24} sm={12} md={6}>
              <Card className="stat-card" bordered={false}>
                <div className="stat-row">
                  <div className="icon-box bg-green">
                    <BarChartOutlined />
                  </div>
                  <div className="stat-info">
                    <div className="label">平均总分</div>
                    <div className="value">{stats.avg}</div>
                  </div>
                </div>
              </Card>
            </Col>
            <Col xs={24} sm={12} md={6}>
              <Card className="stat-card" bordered={false}>
                <div className="stat-row">
                  <div className="icon-box bg-orange">
                    <TrophyOutlined />
                  </div>
                  <div className="stat-info">
                    <div className="label">最高分</div>
                    <div className="value">{stats.max}</div>
                  </div>
                </div>
              </Card>
            </Col>
            <Col xs={24} sm={12} md={6}>
              <Card className="stat-card" bordered={false}>
                <div className="stat-row">
                  <div className="icon-box bg-red">
                    <WarningOutlined />
                  </div>
                  <div className="stat-info">
                    <div className="label">低分预警(&lt;60)</div>
                    <div className="value">{stats.warningCount}</div>
                  </div>
                </div>
              </Card>
            </Col>
          </Row>

          <Row gutter={[16, 16]} className="mt-16">
            <Col xs={24} md={12}>
              <div ref={chartContainer} className="chart-panel" />
            </Col>
            <Col xs={24} md={12}>
              <div ref={radarContainer} className="chart-panel" />
            </Col>
          </Row>
        </div>
      )}

      <Alert
        message={`班级：${className}，数据分析如上；点击下方按钮导出 Excel（105 分制）`}
        type="success"
        showIcon
        className="mb-16"
      />

      {students.length > 0 && (
        <SimpleTable
          className="mt-16"
          data={students}
          columns={[
            ...baseColumns,
            { title: '智育(70)', dataIndex: 'gradeScore', key: 'gradeScore', width: 110 },
            { title: '奖励(5)', dataIndex: 'rewardScore', key: 'rewardScore', width: 100 },
            { title: '体育(5)', dataIndex: 'sportsScore', key: 'sportsScore', width: 100 },
            { title: '德育(15)', dataIndex: 'moralScore', key: 'moralScore', width: 110 },
            { title: '劳育(5)', dataIndex: 'laborScore', key: 'laborScore', width: 100 },
            { title: '美育(5)', dataIndex: 'aestheticScore', key: 'aestheticScore', width: 100 },
            { title: '综合(105)', dataIndex: 'total', key: 'total', width: 120 }
          ]}
        />
      )}

      <div className="footer-bar">
        <Space>
          <Button onClick={onPrev}>上一步</Button>
          <Button type="primary" onClick={onExportExcel}>
            导出 Excel 汇总
          </Button>
        </Space>
      </div>
    </div>
  )
}

export default Step5
