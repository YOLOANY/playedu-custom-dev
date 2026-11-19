import { InputNumber, Tree, Tooltip } from "antd";
import { useState, useEffect } from "react";
import type { DataNode, TreeProps } from "antd/es/tree";

interface Option {
  key: string | number;
  title: any;
}

interface PropInterface {
  data: CourseHourModel[];
  onRemoveItem: (id: number) => void;
  onUpdate: (arr: any[]) => void;
  onCreditChange?: (hourKey: number, credit: number) => void;
  onCreditSave?: (hourKey: number) => void;
}

export const TreeHours = (props: PropInterface) => {
  const [treeData, setTreeData] = useState<Option[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  useEffect(() => {
    const hours = props.data;
    if (hours.length === 0) {
      setTreeData([]);
      return;
    }
    checkTree(hours);
  }, [props.data]);

  const checkTree = (hours: any) => {
    const arr = [];
    for (let i = 0; i < hours.length; i++) {
      const rowKey = hours[i].id ?? hours[i].rid;
      arr.push({
        title: (
          <div
            className="d-flex"
            style={{ alignItems: "center", width: "100%", gap: 12 }}
          >
            <div
              className="d-flex"
              style={{ alignItems: "center", flex: 1, minWidth: 0 }}
            >
              <i
                className="iconfont icon-icon-video"
                style={{
                  fontSize: 16,
                  color: "rgba(0,0,0,0.3)",
                  flexShrink: 0,
                }}
              />
              <div
                className="tree-video-title"
                style={{
                  flex: 1,
                  width: "auto",
                  minWidth: 0,
                  overflow: "hidden",
                  textOverflow: "ellipsis",
                  whiteSpace: "nowrap",
                }}
              >
                {hours[i].name}
              </div>
            </div>
            <div
              className="d-flex"
              style={{
                alignItems: "center",
                justifyContent: "flex-end",
                gap: 8,
                flex: "0 0 128px",
              }}
            >
              <span
                className="helper-text"
                style={{ whiteSpace: "nowrap", flexShrink: 0 }}
              >
                学分
              </span>
              <InputNumber
                min={0}
                precision={0}
                controls={false}
                size="small"
                style={{ width: 88 }}
                value={hours[i].credit ?? 0}
                onChange={(value) =>
                  props.onCreditChange?.(rowKey, value ?? 0)
                }
                onBlur={() => props.onCreditSave?.(rowKey)}
                onMouseDown={(event) => event.stopPropagation()}
              />
            </div>
            <Tooltip placement="top" title="可拖拽排序">
              <i
                className="iconfont icon-icon-drag"
                style={{ fontSize: 24, flexShrink: 0 }}
              />
            </Tooltip>
            <i
              className="iconfont icon-icon-delete"
              style={{ fontSize: 24, flexShrink: 0 }}
              onClick={() => removeItem(rowKey)}
            />
          </div>
        ),
        key: rowKey,
      });
    }
    setTreeData(arr);
  };

  const removeItem = (id: number) => {
    if (id === 0) {
      return;
    }
    props.onRemoveItem(id);
  };

  const onDrop: TreeProps["onDrop"] = (info) => {
    const dropKey = info.node.key;
    const dragKey = info.dragNode.key;
    const dropPos = info.node.pos.split("-");
    const dropPosition =
      info.dropPosition - Number(dropPos[dropPos.length - 1]);
    const loop = (
      data: DataNode[],
      key: React.Key,
      callback: (node: DataNode, i: number, data: DataNode[]) => void
    ) => {
      for (let i = 0; i < data.length; i++) {
        if (data[i].key === key) {
          return callback(data[i], i, data);
        }
        if (data[i].children) {
          loop(data[i].children!, key, callback);
        }
      }
    };
    const data = [...treeData];
    let isTop = false;

    for (let i = 0; i < data.length; i++) {
      if (data[i].key === dragKey) {
        isTop = true;
      }
    }

    // Find dragObject
    let dragObj: DataNode;
    let dragLength = (info.dragNode as any).props.pos.split("-").length;
    let dropLength = (info.node as any).props.pos.split("-").length;

    if (!info.dropToGap) {
      // Drop on the content
      if (
        (dropPosition == 0 && dropPos.length == 3) ||
        (dropPosition == 0 && dropPos.length == 2 && dragLength == 2)
      ) {
      } else {
        loop(data, dragKey, (item, index, arr) => {
          arr.splice(index, 1);
          dragObj = item;
        });
        loop(data, dropKey, (item) => {
          item.children = item.children || [];
          // where to insert 示例添加到头部，可以是随意位置
          item.children.unshift(dragObj);
        });
      }
    } else if (
      ((info.node as any).props.children || []).length > 0 && // Has children
      (info.node as any).props.expanded && // Is expanded
      dropPosition === 1 // On the bottom gap
    ) {
      loop(data, dragKey, (item, index, arr) => {
        arr.splice(index, 1);
        dragObj = item;
      });
      loop(data, dropKey, (item) => {
        item.children = item.children || [];
        // where to insert 示例添加到头部，可以是随意位置
        item.children.unshift(dragObj);
        // in previous version, we use item.children.push(dragObj) to insert the
        // item to the tail of the children
      });
    } else {
      if (
        (dragLength == 3 && dropLength == 2) ||
        (dragLength == 2 && dropLength == 3)
      ) {
        return;
      }
      loop(data, dragKey, (item, index, arr) => {
        arr.splice(index, 1);
        dragObj = item;
      });
      let ar: DataNode[] = [];
      let i: number;
      loop(data, dropKey, (_item, index, arr) => {
        ar = arr;
        i = index;
      });
      if (dropPosition === -1) {
        ar.splice(i!, 0, dragObj!);
      } else {
        ar.splice(i! + 1, 0, dragObj!);
      }
    }
    setTreeData(data);
    const keys = data.map((item: any) => item.key);
    props.onUpdate(keys);
  };

  const onDragEnter: TreeProps["onDragEnter"] = (info) => {
    console.log(info);
  };

  return (
    <div className="playedu-tree">
      <Tree
        className="playedu-hours-tree"
        draggable
        blockNode
        onDragEnter={onDragEnter}
        onDrop={onDrop}
        selectable={false}
        treeData={treeData}
      />
    </div>
  );
};
