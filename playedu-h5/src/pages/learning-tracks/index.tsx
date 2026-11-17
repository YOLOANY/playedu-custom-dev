import { useEffect, useState } from "react";
import { Image, Skeleton } from "antd-mobile";
import { useNavigate } from "react-router-dom";
import { user } from "../../api";
import { Empty } from "../../components";
import backIcon from "../../assets/images/commen/icon-back.png";
import styles from "./index.module.scss";
import moment from "moment";

const LearningTracksPage = () => {
  const navigate = useNavigate();
  const [tracks, setTracks] = useState<UserLearningTrackModel[]>([]);
  const [page, setPage] = useState(1);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);

  const getTracks = (page: number) => {
    setLoading(true);

    user
      .learningTracks(page, 20)
      .then((res: any) => {
        setTracks([...tracks, ...res.data.data]);
        setPage(page);
        setTotal(res.data.total);
        setLoading(false);
      })
      .catch(() => {
        setLoading(false);
      });
  };

  useEffect(() => {
    document.title = "学习轨迹";
    getTracks(1);
  }, []);

  const loadMore = () => {
    if (loading) {
      return;
    }

    getTracks(page + 1);
  };

  const hasMore = tracks.length < total;

  const groupedTracks: Record<string, UserLearningTrackModel[]> = {};

  tracks.forEach((item) => {
    const month = moment(item.finished_at).format("YYYY-MM");

    if (!groupedTracks[month]) {
      groupedTracks[month] = [];
    }

    groupedTracks[month].push(item);
  });

  return (
    <div className="main-body">
      <div className={`main-header ${styles["sticky-header"]}`}>
        <Image
          className="back-icon"
          src={backIcon}
          onClick={() => navigate(-1)}
        />
        <div className="main-title">学习轨迹</div>
      </div>
      <div className={styles["content"]}>
        <div className={styles["summary"]}>
          共完成 <strong>{total}</strong> 个课时
        </div>
        {loading && tracks.length === 0 && (
          <div className={styles["loading-box"]}>
            <Skeleton animated style={{ width: "60%", height: 24 }} />
            <Skeleton animated style={{ width: "80%", height: 20 }} />
            <Skeleton animated style={{ width: "50%", height: 20 }} />
          </div>
        )}
        {!loading && tracks.length === 0 && <Empty />}
        {tracks.length > 0 &&
          Object.entries(groupedTracks).map(([month, items]) => (
            <div key={month}>
              <div className={styles["month-title"]}>{month}</div>
              {items.map((item) => (
                <div className={styles["track-item"]} key={item.record_id}>
                  <div className={styles["dot"]} />
                  <div className={styles["track-content"]}>
                    <div className={styles["course-title"]}>{item.course_title}</div>
                    <div className={styles["hour-title"]}>{item.hour_title}</div>
                    <div className={styles["finished-at"]}>
                      {moment(item.finished_at).format("YYYY年MM月DD日 HH:mm")}
                    </div>
                  </div>
                </div>
              ))}
            </div>
          ))}
        {tracks.length > 0 && hasMore && (
          <div className={styles["load-more"]} onClick={loadMore}>
            {loading ? "加载中..." : "加载更多"}
          </div>
        )}
        {!loading && tracks.length > 0 && !hasMore && (
          <div className={styles["list-end"]}>没有更多了</div>
        )}
      </div>
    </div>
  );
};

export default LearningTracksPage;
