#!/usr/bin/env python3
import pandas as pd
import json
from datetime import datetime

# Excel 파일 읽기
file_path = './item/doubless/doubless.xlsx'

try:
    # 여러 시트가 있을 수 있으므로 모든 시트 확인
    xl_file = pd.ExcelFile(file_path)
    print(f"발견된 시트: {xl_file.sheet_names}")
    
    # 첫 번째 시트 읽기
    df = pd.read_excel(file_path, sheet_name=0)
    
    print("\n=== 데이터 개요 ===")
    print(f"총 행 수: {len(df)}")
    print(f"총 열 수: {len(df.columns)}")
    print(f"\n컬럼명: {df.columns.tolist()}")
    
    print("\n=== 첫 5개 행 ===")
    print(df.head())
    
    print("\n=== 데이터 타입 ===")
    print(df.dtypes)
    
    print("\n=== 기본 통계 ===")
    print(df.describe())
    
    # 데이터를 JSON으로 저장 (웹에서 사용하기 위해)
    df_json = df.to_json(orient='records', force_ascii=False)
    
    with open('./item/doubless/sales_data.json', 'w', encoding='utf-8') as f:
        f.write(df_json)
    
    print("\n✅ 데이터가 sales_data.json으로 변환되었습니다.")
    
except Exception as e:
    print(f"❌ 오류 발생: {e}")