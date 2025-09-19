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

    def close(self):
        # 이 메서드는 현재 구현에서 사용되지 않지만, main.py의 lifespan 함수를 위해 남겨둡니다.
        # 이 클래스 내의 모든 메서드는 연결 후 바로 conn.close()를 호출합니다.
        pass
    
    def get_ad_content_sync(self, ad_content_id: int):
        """
        ad_contents 테이블에서 특정 id의 데이터를 불러옵니다.
        """
        conn = None
        data = None
        try:
            conn = self.get_db_connection()
            with conn.cursor() as cursor:
                # 'analysis_result'를 'ad_text'로 수정했습니다.
                sql = "SELECT original_image_base64, product, ad_text FROM ad_contents WHERE id = %s"
                cursor.execute(sql, (ad_content_id,))
                data = cursor.fetchone()
        finally:
            if conn:
                conn.close()

        if not data:
            return None

        return {
            "original_image_base64": data.get("original_image_base64"),
            "product_name": data.get("product"),
            "qwen_analysis": data.get("ad_text")
        }

    def update_final_ad_sync(self, ad_content_id: int, final_image_base64: str):
        """
        ad_contents 테이블의 generated_image_base64 필드를 업데이트합니다.
        """
        conn = None
        try:
            conn = self.get_db_connection()
            with conn.cursor() as cursor:
                sql = "UPDATE ad_contents SET generated_image_base64 = %s WHERE id = %s"
                cursor.execute(sql, (final_image_base64, ad_content_id))
            conn.commit()
            return True
        except Exception as e:
            print(f"Database update failed: {e}")
            return False
        finally:
            if conn:
                conn.close()