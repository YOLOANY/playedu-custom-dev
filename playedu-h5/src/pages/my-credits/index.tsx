import { useEffect, useRef, useState } from "react";
import { Image, Skeleton } from "antd-mobile";
import { useNavigate } from "react-router-dom";
import moment from "moment";
import { user } from "../../api";
import { Empty } from "../../components";
import backIcon from "../../assets/images/commen/icon-back.png";
import styles from "./index.module.scss";

const MyCreditsPage = () => {
  const navigate = useNavigate();
  const [records, setRecords] = useState<UserCreditRecordModel[]>([]);
  const [summary, setSummary] = useState<UserCreditSummaryModel | null>(null);
  const [page, setPage] = useState(1);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);
  const loadingRef = useRef(false);

  const getCredits = (nextPage: number) => {
    if (loadingRef.current) {
      return;
    }

    loadingRef.current = true;
    setLoading(true);

    user
      .credits(nextPage, 20)
      .then((res: any) => {
        const data = res.data as UserCreditsModel;
        setRecords((currentRecords) =>
          nextPage === 1 ? data.data : [...currentRecords, ...data.data]
        );
        setSummary({
          total_credit: data.total_credit,
          semester_credit: data.semester_credit,
          semester: data.semester,
          semester_start: data.semester_start,
          semester_end: data.semester_end,
          time_zone: data.time_zone,
        });
        setPage(nextPage);
        setTotal(data.total);
      })
      .catch(() => {})
      .finally(() => {
        loadingRef.current = false;
        setLoading(false);
      });
  };

  useEffect(() => {
    document.title = "我的学分";
    getCredits(1);
  }, []);

  const loadMore = () => {
    if (loadingRef.current) {
      return;
    }

    getCredits(page + 1);
  };

  const hasMore = records.length < total;

  return (
    <div className={`main-body ${styles["page"]}`}>
      <div className={`main-header ${styles["sticky-header"]}`}>
        <Image
          className="back-icon"
          src={backIcon}
          onClick={() => navigate(-1)}
        />
        <div className="main-title">我的学分</div>
      </div>
      <div className={styles["content"]}>
        {summary && (
          <div className={styles["summary"]}>
            <div className={styles["summary-card"]}>
              <div className={styles["credit-number"]}>
                {summary.total_credit}
              </div>
              <div className={styles["summary-label"]}>累计总学分</div>
            </div>
            <div className={styles["summary-card"]}>
              <div className={styles["credit-number"]}>
                {summary.semester_credit}
              </div>
              <div className={styles["summary-label"]}>本学期获得学分</div>
              {summary.semester && (
                <div className={styles["semester-name"]}>{summary.semester}</div>
              )}
            </div>
          </div>
        )}
        {loading && records.length === 0 && (
          <div className={styles["loading-box"]}>
            <Skeleton animated style={{ width: "55%", height: 32 }} />
            <Skeleton animated style={{ width: "100%", height: 88 }} />
            <Skeleton animated style={{ width: "100%", height: 88 }} />
            <Skeleton animated style={{ width: "100%", height: 88 }} />
          </div>
        )}
        {!loading && records.length === 0 && <Empty />}
        {records.length > 0 && (
          <>
            <div className={styles["section-title"]}>学分明细</div>
            <div className={styles["credit-list"]}>
              {records.map((item) => (
                <div className={styles["credit-item"]} key={item.id}>
                  <div className={styles["credit-info"]}>
                    <div className={styles["course-title"]}>
                      {item.course_title}
                    </div>
                    {item.hour_title && (
                      <div className={styles["hour-title"]}>
                        {item.hour_title}
                      </div>
                    )}
                    <div className={styles["award-time"]}>
                      {moment(item.created_at).format("YYYY年MM月DD日 HH:mm")}
                    </div>
                  </div>
                  <div className={styles["credit-value"]}>+{item.credit}</div>
                </div>
              ))}
            </div>
          </>
        )}
        {records.length > 0 && hasMore && (
          <div className={styles["load-more"]} onClick={loadMore}>
            {loading ? "加载中..." : "加载更多"}
          </div>
        )}
        {!loading && records.length > 0 && !hasMore && (
          <div className={styles["list-end"]}>没有更多了</div>
        )}
      </div>
    </div>
  );
};

export default MyCreditsPage;
