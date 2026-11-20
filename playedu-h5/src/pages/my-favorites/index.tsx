import { useEffect, useRef, useState } from "react";
import { Image, Skeleton } from "antd-mobile";
import { useNavigate } from "react-router-dom";
import { user } from "../../api";
import { Empty } from "../../components";
import backIcon from "../../assets/images/commen/icon-back.png";
import defaultThumb1 from "../../assets/thumb/thumb1.png";
import defaultThumb2 from "../../assets/thumb/thumb2.png";
import defaultThumb3 from "../../assets/thumb/thumb3.png";
import styles from "./index.module.scss";

const MyFavoritesPage = () => {
  const navigate = useNavigate();
  const [records, setRecords] = useState<CourseModel[]>([]);
  const [resourceUrl, setResourceUrl] = useState<ResourceUrlModel>({});
  const [page, setPage] = useState(1);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);
  const loadingRef = useRef(false);

  const getFavorites = (nextPage: number) => {
    if (loadingRef.current) {
      return;
    }

    loadingRef.current = true;
    setLoading(true);

    user
      .favorites(nextPage, 20)
      .then((res: any) => {
        const data = res.data as UserFavoritesModel;
        setRecords((currentRecords) =>
          nextPage === 1 ? data.data : [...currentRecords, ...data.data]
        );
        setResourceUrl((currentResourceUrl) =>
          nextPage === 1
            ? data.resource_url
            : { ...currentResourceUrl, ...data.resource_url }
        );
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
    document.title = "我的收藏";
    getFavorites(1);
  }, []);

  const loadMore = () => {
    if (loadingRef.current) {
      return;
    }

    getFavorites(page + 1);
  };

  const hasMore = records.length < total;

  const getThumb = (item: CourseModel) => {
    if (item.thumb === -1) {
      return defaultThumb1;
    }
    if (item.thumb === -2) {
      return defaultThumb2;
    }
    if (item.thumb === -3) {
      return defaultThumb3;
    }
    return resourceUrl[item.thumb] || defaultThumb1;
  };

  return (
    <div className={`main-body ${styles["page"]}`}>
      <div className={`main-header ${styles["sticky-header"]}`}>
        <Image
          className="back-icon"
          src={backIcon}
          onClick={() => navigate(-1)}
        />
        <div className="main-title">我的收藏</div>
      </div>
      <div className={styles["content"]}>
        {loading && records.length === 0 && (
          <div className={styles["loading-box"]}>
            <Skeleton animated style={{ width: "100%", height: 114 }} />
            <Skeleton animated style={{ width: "100%", height: 114 }} />
            <Skeleton animated style={{ width: "100%", height: 114 }} />
          </div>
        )}
        {!loading && records.length === 0 && <Empty />}
        {records.length > 0 && (
          <div className={styles["favorite-list"]}>
            {records.map((item) => (
              <div
                className={styles["favorite-item"]}
                key={item.id}
                onClick={() => navigate(`/course/${item.id}`)}
              >
                <Image
                  className={styles["course-thumb"]}
                  src={getThumb(item)}
                  width={120}
                  height={90}
                  fit="cover"
                />
                <div className={styles["favorite-info"]}>
                  <div className={styles["course-title"]}>{item.title}</div>
                  <div className={styles["course-meta"]}>
                    <span className={styles["course-type"]}>
                      {item.is_required === 1 ? "必修课" : "选修课"}
                    </span>
                    <span className={styles["course-hours"]}>
                      {item.class_hour}课时
                    </span>
                  </div>
                </div>
              </div>
            ))}
          </div>
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

export default MyFavoritesPage;
