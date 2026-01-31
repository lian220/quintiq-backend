#!/usr/bin/env python3
"""
Stock 데이터 마이그레이션 스크립트
Local stock-trading (MongoDB Atlas) → Quantiq (Local MongoDB)
"""
import sys
from pymongo import MongoClient
from datetime import datetime

# Source: Local stock-trading (MongoDB Atlas)
SOURCE_URI = "mongodb+srv://test:6n2AB4V2halcSvfv@cluster-test.2dkjwjs.mongodb.net"
SOURCE_DB = "stock_trading"
SOURCE_COLLECTION = "daily_stock_data"

# Target: Quantiq (Local MongoDB)
TARGET_URI = "mongodb://quantiq_user:quantiq_password@localhost:27017"
TARGET_DB = "stock_trading"
TARGET_COLLECTION = "daily_stock_data"
TARGET_AUTH_DB = "admin"


def connect_to_source():
    """Source MongoDB에 연결"""
    print("📡 Source MongoDB (Atlas) 연결 중...")
    client = MongoClient(SOURCE_URI)
    db = client[SOURCE_DB]
    collection = db[SOURCE_COLLECTION]

    # 연결 테스트
    count = collection.count_documents({})
    print(f"✅ Source 연결 성공: {count}개 문서")

    return client, db, collection


def connect_to_target():
    """Target MongoDB에 연결"""
    print("📡 Target MongoDB (Local) 연결 중...")
    client = MongoClient(
        TARGET_URI,
        authSource=TARGET_AUTH_DB
    )
    db = client[TARGET_DB]
    collection = db[TARGET_COLLECTION]

    # 연결 테스트
    count = collection.count_documents({})
    print(f"✅ Target 연결 성공: {count}개 문서")

    return client, db, collection


def analyze_stocks_data(source_collection):
    """stocks 데이터 분석"""
    print("\n📊 Source 데이터 분석 중...")

    # stocks 필드가 있는 문서 개수
    with_stocks = source_collection.count_documents({"stocks": {"$exists": True, "$ne": {}}})
    print(f"   - stocks 필드가 있는 문서: {with_stocks}개")

    # 샘플 데이터 확인
    sample = source_collection.find_one({"stocks": {"$exists": True, "$ne": {}}})
    if sample:
        print(f"   - 샘플 날짜: {sample.get('date')}")
        stocks_count = len(sample.get('stocks', {}).keys())
        print(f"   - 주식 개수: {stocks_count}개")
        print(f"   - 주식 목록: {', '.join(list(sample.get('stocks', {}).keys())[:10])}...")

    return with_stocks


def migrate_stocks_data(source_collection, target_collection, dry_run=True):
    """stocks 데이터 마이그레이션"""
    print(f"\n🔄 마이그레이션 시작 {'(DRY RUN)' if dry_run else '(LIVE)'}")

    # stocks 필드가 있는 문서 조회
    cursor = source_collection.find(
        {"stocks": {"$exists": True, "$ne": {}}},
        {"date": 1, "stocks": 1}
    ).sort("date", 1)

    updated_count = 0
    skipped_count = 0
    error_count = 0

    for doc in cursor:
        date = doc.get("date")
        stocks = doc.get("stocks", {})

        if not date or not stocks:
            skipped_count += 1
            continue

        try:
            if dry_run:
                # Dry run: Target에 해당 날짜가 있는지만 확인
                exists = target_collection.find_one({"date": date}, {"_id": 1})
                if exists:
                    print(f"   ✓ {date}: {len(stocks)}개 주식 (시뮬레이션)")
                    updated_count += 1
                else:
                    print(f"   ⚠ {date}: Target에 없음")
                    skipped_count += 1
            else:
                # Live: 실제 업데이트
                result = target_collection.update_one(
                    {"date": date},
                    {
                        "$set": {
                            "stocks": stocks,
                            "stocks_migrated_at": datetime.now()
                        }
                    }
                )

                if result.matched_count > 0:
                    print(f"   ✅ {date}: {len(stocks)}개 주식 업데이트")
                    updated_count += 1
                else:
                    print(f"   ⚠ {date}: Target에 없음 (스킵)")
                    skipped_count += 1

        except Exception as e:
            print(f"   ❌ {date}: 오류 - {e}")
            error_count += 1

    print(f"\n📊 마이그레이션 결과:")
    print(f"   - 업데이트: {updated_count}개")
    print(f"   - 스킵: {skipped_count}개")
    print(f"   - 오류: {error_count}개")

    return updated_count, skipped_count, error_count


def main():
    """메인 함수"""
    print("=" * 80)
    print("Stock 데이터 마이그레이션")
    print("Local stock-trading (Atlas) → Quantiq (Local)")
    print("=" * 80)

    # 명령행 인자 확인
    dry_run = "--live" not in sys.argv
    if dry_run:
        print("\n⚠️  DRY RUN 모드: 실제 데이터 변경 없음")
        print("   실제 마이그레이션: python migrate_stocks_data.py --live\n")
    else:
        print("\n🚨 LIVE 모드: 실제 데이터 변경됨\n")
        response = input("계속하시겠습니까? (yes/no): ")
        if response.lower() != "yes":
            print("❌ 취소됨")
            return

    source_client = None
    target_client = None

    try:
        # 연결
        source_client, source_db, source_collection = connect_to_source()
        target_client, target_db, target_collection = connect_to_target()

        # 분석
        with_stocks = analyze_stocks_data(source_collection)

        if with_stocks == 0:
            print("\n⚠️  Source에 stocks 데이터가 없습니다.")
            return

        # 마이그레이션
        updated, skipped, errors = migrate_stocks_data(
            source_collection,
            target_collection,
            dry_run=dry_run
        )

        if dry_run:
            print("\n✅ DRY RUN 완료")
            print(f"   실제 마이그레이션 시: {updated}개 문서 업데이트 예상")
        else:
            print("\n✅ 마이그레이션 완료!")
            print(f"   총 {updated}개 날짜에 stocks 데이터 추가됨")

    except Exception as e:
        print(f"\n❌ 오류 발생: {e}")
        import traceback
        traceback.print_exc()

    finally:
        # 연결 종료
        if source_client:
            source_client.close()
            print("\n📡 Source 연결 종료")
        if target_client:
            target_client.close()
            print("📡 Target 연결 종료")


if __name__ == "__main__":
    main()
