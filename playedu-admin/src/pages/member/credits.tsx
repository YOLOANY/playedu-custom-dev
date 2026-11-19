import { useEffect, useState } from "react";
import { Alert, Table, Tabs } from "antd";
import type { ColumnsType } from "antd/es/table";
import { useSelector } from "react-redux";
import { user } from "../../api";

interface CreditUser {
  id: number;
  name: string;
  email: string;
  credit1: number;
}

interface DepartmentCredit {
  department_id: number;
  department_name: string;
  parent_id: number;
  user_count: number;
  total_credit: number;
}

const MemberCreditsPage = () => {
  const permissions = useSelector(
    (state: any) => state.loginUser.value.permissions
  );
  const [page, setPage] = useState(1);
  const [size, setSize] = useState(10);
  const [total, setTotal] = useState(0);
  const [ranking, setRanking] = useState<CreditUser[]>([]);
  const [rankingLoading, setRankingLoading] = useState(false);
  const [departments, setDepartments] = useState<Record<number, string>>({});
  const [userDepartments, setUserDepartments] = useState<
    Record<number, number[]>
  >({});
  const [summary, setSummary] = useState<DepartmentCredit[]>([]);
  const [summaryLoading, setSummaryLoading] = useState(false);
  const canViewSummary =
    typeof permissions["department-user-learn"] !== "undefined";

  useEffect(() => {
    setRankingLoading(true);
    user
      .creditRanking(page, size)
      .then((res: any) => {
        setRanking(res.data.data);
        setTotal(res.data.total);
        setDepartments(res.data.departments);
        setUserDepartments(res.data.user_dep_ids);
      })
      .finally(() => setRankingLoading(false));
  }, [page, size]);

  useEffect(() => {
    if (!canViewSummary) {
      return;
    }
    setSummaryLoading(true);
    user
      .departmentCreditSummary()
      .then((res: any) => setSummary(res.data))
      .finally(() => setSummaryLoading(false));
  }, [canViewSummary]);

  const rankingColumns: ColumnsType<CreditUser> = [
    {
      title: "排名",
      width: 90,
      render: (_, __, index) => (page - 1) * size + index + 1,
    },
    {
      title: "姓名",
      dataIndex: "name",
      width: 180,
    },
    {
      title: "邮箱",
      dataIndex: "email",
      width: 240,
    },
    {
      title: "班级",
      dataIndex: "id",
      render: (id: number) => {
        const ids = userDepartments[id] || [];
        return ids.length > 0
          ? ids.map((departmentId) => departments[departmentId]).join("、")
          : "未分班";
      },
    },
    {
      title: "累计学分",
      dataIndex: "credit1",
      width: 140,
    },
  ];

  const summaryColumns: ColumnsType<DepartmentCredit> = [
    {
      title: "班级名称",
      dataIndex: "department_name",
    },
    {
      title: "学员人数",
      dataIndex: "user_count",
      width: 180,
    },
    {
      title: "累计总学分",
      dataIndex: "total_credit",
      width: 180,
    },
  ];

  const tabItems = [
    {
      key: "ranking",
      label: "全院学生学分排名",
      children: (
        <Table
          rowKey="id"
          columns={rankingColumns}
          dataSource={ranking}
          loading={rankingLoading}
          pagination={{
            current: page,
            pageSize: size,
            total,
            showSizeChanger: true,
            onChange: (nextPage, nextSize) => {
              setPage(nextPage);
              setSize(nextSize);
            },
          }}
        />
      ),
    },
  ];

  if (canViewSummary) {
    tabItems.push({
      key: "summary",
      label: "各班级学分汇总",
      children: (
        <>
          <Alert
            className="mb-24"
            type="info"
            showIcon
            message="每个班级均统计本班及全部下级班级，同一统计范围内学员去重。父子班级是不同统计口径，不能将所有行相加作为全院总学分。"
          />
          <Table
            rowKey="department_id"
            columns={summaryColumns}
            dataSource={summary}
            loading={summaryLoading}
            pagination={false}
          />
        </>
      ),
    });
  }

  return (
    <div className="playedu-main-body">
      <div className="playedu-main-title float-left mb-24">学分统计</div>
      <div className="float-left">
        <Tabs items={tabItems} />
      </div>
    </div>
  );
};

export default MemberCreditsPage;
