import { useMemo } from 'react'
import type { CSSProperties, ReactNode } from 'react'
import { FixedSizeList, type ListChildComponentProps } from 'react-window'
import type { StudentSummary } from '../types'

export interface SimpleColumn {
  key: string
  title: string
  width?: number
  dataIndex?: keyof StudentSummary
  render?: (row: StudentSummary, index: number) => ReactNode
}

interface Props {
  data: StudentSummary[]
  columns: SimpleColumn[]
  className?: string
}

const VIRTUAL_THRESHOLD = 120
const ROW_HEIGHT = 42
const HEADER_HEIGHT = 42
const OVERSCAN_COUNT = 8
const MAX_BODY_HEIGHT = 520
const DEFAULT_COL_WIDTH = 140

interface RowData {
  data: StudentSummary[]
  columns: SimpleColumn[]
  widths: number[]
}

const VirtualRow = ({ index, style, data }: ListChildComponentProps<RowData>) => {
  const row = data.data[index]
  return (
    <div className={`simple-v-row ${index % 2 === 1 ? 'even' : ''}`} style={style}>
      {data.columns.map((column: SimpleColumn, colIndex: number) => {
        const value = column.render
          ? column.render(row, index)
          : column.dataIndex
            ? (row[column.dataIndex] as ReactNode)
            : null
        return (
          <div
            key={`${column.key}-${index}`}
            className="simple-v-cell"
            style={{ width: data.widths[colIndex] } as CSSProperties}
          >
            {value}
          </div>
        )
      })}
    </div>
  )
}

const SimpleTable = ({ data, columns, className }: Props) => {
  const useVirtual = data.length > VIRTUAL_THRESHOLD
  const widths = useMemo(
    () => columns.map((column) => column.width ?? DEFAULT_COL_WIDTH),
    [columns]
  )
  const totalWidth = useMemo(
    () => widths.reduce((sum, width) => sum + width, 0),
    [widths]
  )

  if (useVirtual) {
    const bodyHeight = Math.min(MAX_BODY_HEIGHT, data.length * ROW_HEIGHT)

    return (
      <div className={`simple-table-wrap ${className ?? ''}`}>
        <div className="simple-v-table" style={{ minWidth: totalWidth }}>
          <div className="simple-v-header" style={{ height: HEADER_HEIGHT }}>
            {columns.map((column, index) => (
              <div key={column.key} className="simple-v-head-cell" style={{ width: widths[index] }}>
                {column.title}
              </div>
            ))}
          </div>

          <FixedSizeList<RowData>
            height={bodyHeight}
            width={totalWidth}
            itemCount={data.length}
            itemSize={ROW_HEIGHT}
            overscanCount={OVERSCAN_COUNT}
            itemData={{ data, columns, widths }}
          >
            {VirtualRow}
          </FixedSizeList>
        </div>
      </div>
    )
  }

  return (
    <div className={`simple-table-wrap ${className ?? ''}`}>
      <table className="simple-table">
        <thead>
          <tr>
            {columns.map((column) => (
              <th key={column.key} style={column.width ? { width: column.width } : undefined}>
                {column.title}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {data.map((row, index) => (
            <tr key={row.studentNo || `${row.name}-${index}`}>
              {columns.map((column) => {
                const value = column.render
                  ? column.render(row, index)
                  : column.dataIndex
                    ? (row[column.dataIndex] as ReactNode)
                    : null
                return <td key={column.key}>{value}</td>
              })}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

export default SimpleTable
