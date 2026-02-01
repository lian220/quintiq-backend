#!/usr/bin/env python3
"""
Production MongoDB → Local MongoDB 동기화 스크립트
필요한 초기 설정 데이터를 production에서 가져옴
"""
import sys
from pymongo import MongoClient
from datetime import datetime
import logging

# Setup logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

# Production MongoDB (Atlas)
PROD_URI = "mongodb+srv://test:6n2AB4V2halcSvfv@cluster-test.2dkjwjs.mongodb.net"
PROD_DB = "stock_trading"

# Local MongoDB
LOCAL_URI = "mongodb://quantiq_user:quantiq_password@localhost:27017"
LOCAL_DB = "stock_trading"
LOCAL_AUTH_DB = "admin"


def connect_to_prod():
    """Production MongoDB 연결"""
    logger.info("📡 Production MongoDB (Atlas) 연결 중...")
    client = MongoClient(PROD_URI, serverSelectionTimeoutMS=5000)
    db = client[PROD_DB]
    client.admin.command('ping')
    logger.info("✅ Production MongoDB 연결 성공")
    return client, db


def connect_to_local():
    """Local MongoDB 연결"""
    logger.info("📡 Local MongoDB 연결 중...")
    client = MongoClient(LOCAL_URI, authSource=LOCAL_AUTH_DB)
    db = client[LOCAL_DB]
    client.admin.command('ping')
    logger.info("✅ Local MongoDB 연결 성공")
    return client, db


def analyze_prod_data(prod_db):
    """Production 데이터 분석"""
    logger.info("\n📊 Production 데이터 분석 중...")

    collections = [
        'stocks',
        'daily_stock_data',
        'stock_recommendations',
        'stock_predictions',
        'prediction_results',
        'sentiment_analysis',
        'economic_data',
        'stock_analysis_results'
    ]

    data_summary = {}
    for coll_name in collections:
        try:
            count = prod_db[coll_name].count_documents({})
            data_summary[coll_name] = count
            logger.info(f"   - {coll_name:30s}: {count:6d} documents")
        except Exception as e:
            logger.warning(f"   - {coll_name:30s}: ⚠️  {e}")
            data_summary[coll_name] = 0

    return data_summary


def sync_collection(prod_db, local_db, collection_name, limit=None, dry_run=True):
    """컬렉션 동기화"""
    logger.info(f"\n🔄 {collection_name} 동기화 중...")

    try:
        prod_coll = prod_db[collection_name]
        local_coll = local_db[collection_name]

        # Production 데이터 조회
        query = {}
        cursor = prod_coll.find(query)
        if limit:
            cursor = cursor.limit(limit)

        docs = list(cursor)
        logger.info(f"   - Production: {len(docs)}개 문서 조회")

        if not docs:
            logger.warning(f"   - ⚠️  데이터 없음")
            return 0

        if dry_run:
            logger.info(f"   - DRY RUN: {len(docs)}개 문서 동기화 예정")
            # 샘플 데이터 출력
            if docs:
                sample = docs[0]
                logger.info(f"   - 샘플 필드: {list(sample.keys())[:10]}")
            return len(docs)
        else:
            # 기존 데이터 삭제
            local_coll.delete_many({})
            logger.info(f"   - 기존 데이터 삭제 완료")

            # 새 데이터 삽입
            if docs:
                result = local_coll.insert_many(docs)
                logger.info(f"   - ✅ {len(result.inserted_ids)}개 문서 삽입 완료")
                return len(result.inserted_ids)

    except Exception as e:
        logger.error(f"   - ❌ 오류: {e}")
        return 0


def main():
    """메인 함수"""
    print("=" * 80)
    print("Production MongoDB → Local MongoDB 동기화")
    print("=" * 80)

    # 명령행 인자 확인
    dry_run = "--live" not in sys.argv
    collections_to_sync = []

    # 동기화할 컬렉션 지정
    if "--all" in sys.argv:
        collections_to_sync = [
            'stocks',
            'daily_stock_data',
            'stock_recommendations',
            'prediction_results',
            'sentiment_analysis',
            'economic_data'
        ]
    elif "--stocks-only" in sys.argv:
        collections_to_sync = ['stocks']
    elif "--essential" in sys.argv:
        collections_to_sync = ['stocks', 'daily_stock_data']
    else:
        # 기본: stocks만
        collections_to_sync = ['stocks']

    if dry_run:
        print("\n⚠️  DRY RUN 모드: 실제 데이터 변경 없음")
        print("   옵션:")
        print("     --live          : 실제 동기화 실행")
        print("     --force         : 확인 없이 강제 실행")
        print("     --stocks-only   : stocks만 동기화 (기본)")
        print("     --essential     : stocks + daily_stock_data")
        print("     --all           : 모든 컬렉션")
        print()
    else:
        print(f"\n🚨 LIVE 모드: {', '.join(collections_to_sync)} 동기화")
        if "--force" not in sys.argv:
            response = input("계속하시겠습니까? (yes/no): ")
            if response.lower() != "yes":
                print("❌ 취소됨")
                return
        else:
            print("   --force 옵션: 확인 없이 진행")

    prod_client = None
    local_client = None

    try:
        # 연결
        prod_client, prod_db = connect_to_prod()
        local_client, local_db = connect_to_local()

        # Production 데이터 분석
        analyze_prod_data(prod_db)

        # 동기화
        total_synced = 0
        for coll_name in collections_to_sync:
            synced = sync_collection(prod_db, local_db, coll_name, dry_run=dry_run)
            total_synced += synced

        # 결과
        print("\n" + "=" * 80)
        if dry_run:
            print("✅ DRY RUN 완료")
            print(f"   실제 동기화 시: {total_synced}개 문서 동기화 예상")
            print(f"\n실제 동기화 실행:")
            print(f"   python scripts/sync_from_prod_mongodb.py --stocks-only --live")
        else:
            print("✅ 동기화 완료!")
            print(f"   총 {total_synced}개 문서 동기화됨")
        print("=" * 80)

    except Exception as e:
        logger.error(f"\n❌ 오류 발생: {e}")
        import traceback
        traceback.print_exc()

    finally:
        # 연결 종료
        if prod_client:
            prod_client.close()
            logger.info("\n📡 Production 연결 종료")
        if local_client:
            local_client.close()
            logger.info("📡 Local 연결 종료")


if __name__ == "__main__":
    main()
