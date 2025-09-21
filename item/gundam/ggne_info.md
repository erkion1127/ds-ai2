# SD건담 지 제네레이션 이터널 — 대화형 지식 베이스 (유닛 중심)

본 문서는 대화형 에이전트(Claude 등)가 **GGET**의 유닛/파일럿/서포터/개발(디벨롭) 정보를
대화 형태로 질의응답할 수 있도록 만든 레퍼런스입니다.

## 0) 사용 지침 (에이전트 규칙)
- 답변은 가능한 한 **유닛명(한/영/일), 시리즈, 레어도, 입수처(노획/개발/이벤트/가샤)**를 함께 제시.
- 확정 정보가 없을 때는 “자료 미확인/패치 변동 가능”을 명시.
- 동음이의(예: ‘건담’ 다수) 질문은 **시리즈·파일럿·레어도**로 재질문한 뒤 답변.
- 유닛 비교 요청은 **역할 → 핵심 스킬/특성 → 지형 적성 → 팀 시너지/서포터 상성 → 입수 난이도** 순으로.

---

## 1) 게임 한눈 요약
- **장르**: 건담 시뮬레이션(모바일) / iOS·Android
- **부대 편성**: 유닛+캐릭터 **5세트** + **서포터 1장** = 1개 스쿼드
- **입수 경로**
    - 유닛: **스테이지 노획**, **개발(시리즈별 개발경로도)**
    - 캐릭터: **스카우트(가샤 등)**
    - 서포터: **이벤트 보상 등**
- **강화/육성**: 각종 재료·CAPITAL로 유닛/캐릭터/서포터 강화
- **플레이 모드**: 원작 시나리오 스테이지 + 기간 한정 이벤트 / **오토 모드** 지원

> 메모: 서비스 개시 후 이벤트/신규 참전 작품이 로드맵 형태로 순차 추가됩니다(예: ASTRAY, 0083, 썬더볼트 등).

---

## 2) 데이터 스키마 (YAML)

아래 스키마를 기반으로 유닛/파일럿/서포터/개발경로 데이터를 축적합니다.

```yaml
version: 1.0
last_updated: 2025-09-17

unit_schema:
  # 식별
  unit_id: "<내부 키, 예: 'rx-78-2_ur_01'>"
  names:
    kr: "<코리안 표시명>"
    en: "<English>"
    jp: "<日本語>"
  series: "<작품/기원 시리즈>"
  type: "<MS | MA | Warship | etc>"
  rarity: "<N | R | SR | SSR | UR | EX?>"
  role: "<Shooter | Melee | All-round | Supporter-Unit 등>"
  tags: ["Beam", "Physical", "Transformable", "Newtype", "Flight", "Shield", ...]
  terrain:
    space: "<S/A/B/C>"
    ground: "<S/A/B/C>"
    air: "<S/A/B/C>"
    water: "<S/A/B/C>"
  cost: "<선택: 편성 코스트/가중치가 있을 경우>"
  skills:
    - name: "<스킬/어빌리티명>"
      kind: "<active|passive|leader|link>"
      effect: "<간결한 설명>"
      note: "<조건/제한>"
  traits:
    - "<특성/내장 효과(간단 요약)>"
  synergy:
    pilots_best: ["<파일럿명1>", "<파일럿명2>"]   # 상성 좋은 주 파일럿
    supporter_best: ["<서포터명1>", "<서포터명2>"] # 추천 서포터 시너지
  acquisition:
    methods: ["capture", "develop", "event", "gacha"]
    notes: "<이벤트 한정/교환/로드맵 시점 등>"
  dev_paths:
    # 시리즈별 개발 경로도 상 인접/파생 관계 (알려진 범위만 기입)
    parents: ["<선행 유닛들>"]
    children: ["<파생/개발 결과들>"]
  notes: "<밸런스 패치/주의사항>"

pilot_schema:
  pilot_id: "<키>"
  names: {kr: "", en: "", jp: ""}
  series: ""
  role: "<Attacker | Support | Leader 등>"
  traits: ["<특성/패시브>"]
  skills:
    - {name: "", kind: "", effect: ""}
  best_units: ["<상성 좋은 유닛들>"]
  acquisition: {methods: ["scout", "event"], notes: ""}

supporter_schema:
  supporter_id: "<키>"
  names: {kr: "", en: "", jp: ""}
  leader_skill: "<스쿼드 전역에 적용되는 효과>"
  tags: ["<작품/기믹 태그>"]
  acquisition: {methods: ["event", "exchange", "gacha"], notes: ""}

