rootProject.name = "ds-ai2-rag"

include(
    "modules:common",
    "modules:model",
    "modules:embeddings",
    "modules:vectorstore",
    "modules:retriever",
    "modules:ingestion",
    "modules:rag-core",
    "modules:rag-api",
    "modules:re-ranker",
    "modules:eval",
    "modules:cli"
)