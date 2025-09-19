# mysql_module.py

import pymysql
import os
import json
import base64

class MySQLLoader:
    """
    MySQL RDS에서 데이터를 불러오고 저장하는 클래스.
    """

    def __init__(self):
        self.host = os.environ.get("MYSQL_HOST")
        self.user = os.environ.get("MYSQL_USER")
        self.password = os.environ.get("MYSQL_PASSWORD")
        self.db = os.environ.get("MYSQL_DB")
        self.port = 3306

    def get_db_connection(self):
        return pymysql.connect(
            host=self.host,
            user=self.user,
            password=self.password,
            db=self.db,
            port=self.port,
            charset='utf8mb4',
            cursorclass=pymysql.cursors.DictCursor
        )

    def get_ad_content_sync(self, ad_content_id: int):
        """
        ad_contents 테이블에서 특정 id의 데이터를 불러옵니다.
        """
        conn = None
        data = None
        try:
            conn = self.get_db_connection()
            with conn.cursor() as cursor:
                sql = "SELECT original_image_base64, product, analysis_result FROM ad_contents WHERE id = %s"
                cursor.execute(sql, (ad_content_id,))
                data = cursor.fetchone()
        finally:
            if conn:
                conn.close()

        if not data:
            return None

        return {
            "image_base64": data.get("original_image_base64"),
            "product_name": data.get("product"),
            "qwen_analysis": data.get("analysis_result")
        }

    def update_ad_content_result_sync(self, ad_content_id: int, result: dict):
        """
        ad_contents 테이블에 최종 결과를 JSON 형식으로 저장합니다.
        """
        conn = None
        try:
            conn = self.get_db_connection()
            with conn.cursor() as cursor:
                json_result = json.dumps(result, ensure_ascii=False)
                sql = "UPDATE ad_contents SET final_image_result = %s WHERE id = %s"
                cursor.execute(sql, (json_result, ad_content_id))

            conn.commit()
            return True

        except Exception as e:
            print(f"Database update failed: {e}")
            return False

        finally:
            if conn:
                conn.close()