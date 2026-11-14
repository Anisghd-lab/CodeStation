package com.ghdinteractivestudio.codestation.domain.model

data class AgencyAgent(
    val id: String,
    val name: String,
    val slug: String,
    val division: String,
    val role: String,
    val description: String,
    val skillPath: String = "~/.agents/skills/$slug/SKILL.md"
)

object AgencyAgentCatalog {
    val divisions = listOf(
        "Toutes",
        "Ingénierie & Dev",
        "Qualité & Tests",
        "Cybersécurité",
        "Architecture & Systèmes",
        "Design & UI/UX",
        "Data & AI",
        "Product & Project",
        "GIS & Spatial",
        "Game Development",
        "Stratégie & Ventes",
        "DevOps & Cloud",
        "Conformité & Légal",
        "Mobile & Desktop",
        "Médias & Contenu",
        "International & Langues",
        "Finance & Comptabilité",
        "Support & Client"
    )

    val agents: List<AgencyAgent> = listOf(
        AgencyAgent(
            id = "agency-3d-scene-developer",
            name = "3D Scene Developer",
            slug = "agency-3d-scene-developer",
            division = "Ingénierie & Dev",
            role = "3D Scene Developer",
            description = "Web 3D visualization specialist who creates immersive 3D scenes, terrain models, point cloud visualizations, and interactive web experiences using Cesium, ArcGIS Scene Viewer, and modern 3D web frameworks."
        ),
        AgencyAgent(
            id = "agency-accessibility-auditor",
            name = "Accessibility Auditor",
            slug = "agency-accessibility-auditor",
            division = "Qualité & Tests",
            role = "Accessibility Auditor",
            description = "Expert accessibility specialist who audits interfaces against WCAG standards, tests with assistive technologies, and ensures inclusive design. Defaults to finding barriers — if it''s not tested with a screen reader, it''s not accessible."
        ),
        AgencyAgent(
            id = "agency-account-strategist",
            name = "Account Strategist",
            slug = "agency-account-strategist",
            division = "Architecture & Systèmes",
            role = "Account Strategist",
            description = "Expert post-sale account strategist specializing in land-and-expand execution, stakeholder mapping, QBR facilitation, and net revenue retention. Turns closed deals into long-term platform relationships through systematic expansion planning and multi-threaded account development."
        ),
        AgencyAgent(
            id = "agency-accounts-payable-agent",
            name = "Accounts Payable Agent",
            slug = "agency-accounts-payable-agent",
            division = "Data & AI",
            role = "Accounts Payable Agent",
            description = "Autonomous payment processing specialist that executes vendor payments, contractor invoices, and recurring bills across any payment rail — crypto, fiat, stablecoins. Integrates with AI agent workflows via tool calls."
        ),
        AgencyAgent(
            id = "agency-ad-creative-strategist",
            name = "Ad Creative Strategist",
            slug = "agency-ad-creative-strategist",
            division = "Qualité & Tests",
            role = "Ad Creative Strategist",
            description = "Paid media creative specialist focused on ad copywriting, RSA optimization, asset group design, and creative testing frameworks across Google, Meta, Microsoft, and programmatic platforms. Bridges the gap between performance data and persuasive messaging."
        ),
        AgencyAgent(
            id = "agency-aeo-foundations-architect",
            name = "Aeo Foundations Architect",
            slug = "agency-aeo-foundations-architect",
            division = "Architecture & Systèmes",
            role = "Aeo Foundations Architect",
            description = "Expert in AI Engine Optimization infrastructure — implements llms.txt, AI-aware robots.txt, token-budgeted content, structured Markdown availability, and agent discovery files so AI crawlers, citation engines, and browsing agents can find, parse, and act on your site"
        ),
        AgencyAgent(
            id = "agency-agentic-identity-trust-architect",
            name = "Agentic Identity Trust Architect",
            slug = "agency-agentic-identity-trust-architect",
            division = "Qualité & Tests",
            role = "Agentic Identity Trust Architect",
            description = "Designs identity, authentication, and trust verification systems for autonomous AI agents operating in multi-agent environments. Ensures agents can prove who they are, what they''re authorized to do, and what they actually did."
        ),
        AgencyAgent(
            id = "agency-agentic-search-optimizer",
            name = "Agentic Search Optimizer",
            slug = "agency-agentic-search-optimizer",
            division = "Qualité & Tests",
            role = "Agentic Search Optimizer",
            description = "Expert in WebMCP readiness and agentic task completion — audits whether AI agents can actually accomplish tasks on your site (book, buy, register, subscribe), implements WebMCP declarative and imperative patterns, and measures task completion rates across AI browsing agents"
        ),
        AgencyAgent(
            id = "agency-agents-orchestrator",
            name = "Agents Orchestrator",
            slug = "agency-agents-orchestrator",
            division = "Product & Project",
            role = "Agents Orchestrator",
            description = "Autonomous pipeline manager that orchestrates the entire development workflow. You are the leader of this process."
        ),
        AgencyAgent(
            id = "agency-aging-parent-care-companion",
            name = "Aging Parent Care Companion",
            slug = "agency-aging-parent-care-companion",
            division = "Support & Client",
            role = "Aging Parent Care Companion",
            description = "Compassionate, HIPAA-aligned care coordination and decision-support agent for family caregivers managing an aging parent''s appointments, medications, care team communication, and their own caregiver wellbeing"
        ),
        AgencyAgent(
            id = "agency-ai-citation-strategist",
            name = "Ai Citation Strategist",
            slug = "agency-ai-citation-strategist",
            division = "Qualité & Tests",
            role = "Ai Citation Strategist",
            description = "Expert in AI recommendation engine optimization (AEO/GEO) — audits brand visibility across ChatGPT, Claude, Gemini, and Perplexity, identifies why competitors get cited instead, and delivers content fixes that improve AI citations"
        ),
        AgencyAgent(
            id = "agency-ai-data-remediation-engineer",
            name = "Ai Data Remediation Engineer",
            slug = "agency-ai-data-remediation-engineer",
            division = "Ingénierie & Dev",
            role = "Ai Data Remediation Engineer",
            description = "Specialist in self-healing data pipelines — uses air-gapped local SLMs and semantic clustering to automatically detect, classify, and fix data anomalies at scale. Focuses exclusively on the remediation layer: intercepting bad data, generating deterministic fix logic via Ollama, and guaranteeing zero data loss. Not a general data engineer — a surgical specialist for when your data is broken and the pipeline can''t stop."
        ),
        AgencyAgent(
            id = "agency-ai-engineer",
            name = "Ai Engineer",
            slug = "agency-ai-engineer",
            division = "Ingénierie & Dev",
            role = "Ai Engineer",
            description = "Expert AI/ML engineer specializing in machine learning model development, deployment, and integration into production systems. Focused on building intelligent features, data pipelines, and AI-powered applications with emphasis on practical, scalable solutions."
        ),
        AgencyAgent(
            id = "agency-ai-generated-code-security-auditor",
            name = "Ai Generated Code Security Auditor",
            slug = "agency-ai-generated-code-security-auditor",
            division = "Qualité & Tests",
            role = "Ai Generated Code Security Auditor",
            description = "Security reviewer for AI-generated and vibe-coded apps — hunts the hardcoded secrets, broken row-level security, and prompt-injection sinks that coding assistants ship by default, then drives a scan, fix, and rescan loop with honest, CWE-mapped findings."
        ),
        AgencyAgent(
            id = "agency-analytics-reporter",
            name = "Analytics Reporter",
            slug = "agency-analytics-reporter",
            division = "Design & UI/UX",
            role = "Analytics Reporter",
            description = "Expert data analyst transforming raw data into actionable business insights. Creates dashboards, performs statistical analysis, tracks KPIs, and provides strategic decision support through data visualization and reporting."
        ),
        AgencyAgent(
            id = "agency-anthropologist",
            name = "Anthropologist",
            slug = "agency-anthropologist",
            division = "Architecture & Systèmes",
            role = "Anthropologist",
            description = "Expert in cultural systems, rituals, kinship, belief systems, and ethnographic method — builds culturally coherent societies that feel lived-in rather than invented"
        ),
        AgencyAgent(
            id = "agency-api-platform-engineer",
            name = "Api Platform Engineer",
            slug = "agency-api-platform-engineer",
            division = "Ingénierie & Dev",
            role = "Api Platform Engineer",
            description = "Expert API platform engineer for public and partner APIs — contract-first design (OpenAPI/gRPC), versioning and deprecation policy, SDK generation, API gateway concerns (auth, rate limiting, quotas), and developer-portal DX."
        ),
        AgencyAgent(
            id = "agency-api-tester",
            name = "Api Tester",
            slug = "agency-api-tester",
            division = "Qualité & Tests",
            role = "Api Tester",
            description = "Expert API testing specialist focused on comprehensive API validation, performance testing, and quality assurance across all systems and third-party integrations"
        ),
        AgencyAgent(
            id = "agency-app-store-optimizer",
            name = "App Store Optimizer",
            slug = "agency-app-store-optimizer",
            division = "Ingénierie & Dev",
            role = "App Store Optimizer",
            description = "Expert app store marketing specialist focused on App Store Optimization (ASO), conversion rate optimization, and app discoverability"
        ),
        AgencyAgent(
            id = "agency-application-security-engineer",
            name = "Application Security Engineer",
            slug = "agency-application-security-engineer",
            division = "Cybersécurité",
            role = "Application Security Engineer",
            description = "AppSec specialist who secures the software development lifecycle through threat modeling, secure code review, SAST/DAST integration, and developer security education that makes secure code the default."
        ),
        AgencyAgent(
            id = "agency-ats-validator-architect",
            name = "Ats Validator Architect",
            slug = "agency-ats-validator-architect",
            division = "Qualité & Tests",
            role = "Ats Validator Architect",
            description = "Architect and validator for Applicant Tracking Systems (ATS) and resume parsers. Combines deterministic information retrieval (BM25/TF-IDF and n-grams without AI), quantified Google/IBM X-Y-Z heuristics calibrated by seniority, layout linearization and PDF text layer integrity auditing, regulatory compliance (EU AI Act, NYC LL 144), sub-5ms client-side execution, and Agent-Native BYOK architecture."
        ),
        AgencyAgent(
            id = "agency-automation-governance-architect",
            name = "Automation Governance Architect",
            slug = "agency-automation-governance-architect",
            division = "Qualité & Tests",
            role = "Automation Governance Architect",
            description = "Governance-first architect for business automations (n8n-first) who audits value, risk, and maintainability before implementation."
        ),
        AgencyAgent(
            id = "agency-autonomous-optimization-architect",
            name = "Autonomous Optimization Architect",
            slug = "agency-autonomous-optimization-architect",
            division = "Qualité & Tests",
            role = "Autonomous Optimization Architect",
            description = "Intelligent system governor that continuously shadow-tests APIs for performance while enforcing strict financial and security guardrails against runaway costs."
        ),
        AgencyAgent(
            id = "agency-backend-architect",
            name = "Backend Architect",
            slug = "agency-backend-architect",
            division = "Ingénierie & Dev",
            role = "Backend Architect",
            description = "Senior backend architect specializing in scalable system design, database architecture, API development, and cloud infrastructure. Builds robust, secure, performant server-side applications and microservices"
        ),
        AgencyAgent(
            id = "agency-baidu-seo-specialist",
            name = "Baidu Seo Specialist",
            slug = "agency-baidu-seo-specialist",
            division = "Architecture & Systèmes",
            role = "Baidu Seo Specialist",
            description = "Expert Baidu search optimization specialist focused on Chinese search engine ranking, Baidu ecosystem integration, ICP compliance, Chinese keyword research, and mobile-first indexing for the China market."
        ),
        AgencyAgent(
            id = "agency-behavioral-nudge-engine",
            name = "Behavioral Nudge Engine",
            slug = "agency-behavioral-nudge-engine",
            division = "Ingénierie & Dev",
            role = "Behavioral Nudge Engine",
            description = "Behavioral psychology specialist that adapts software interaction cadences and styles to maximize user motivation and success."
        ),
        AgencyAgent(
            id = "agency-bilibili-content-strategist",
            name = "Bilibili Content Strategist",
            slug = "agency-bilibili-content-strategist",
            division = "Design & UI/UX",
            role = "Bilibili Content Strategist",
            description = "Expert Bilibili marketing specialist focused on UP主 growth, danmaku culture mastery, B站 algorithm optimization, community building, and branded content strategy for China''s leading video community platform."
        ),
        AgencyAgent(
            id = "agency-bim-gis-specialist",
            name = "Bim Gis Specialist",
            slug = "agency-bim-gis-specialist",
            division = "Architecture & Systèmes",
            role = "Bim Gis Specialist",
            description = "Integration specialist who bridges Building Information Modeling and Geographic Information Systems — Revit/IFC data conversion, indoor mapping, digital twin architecture, and facility management data models."
        ),
        AgencyAgent(
            id = "agency-blender-add-on-engineer",
            name = "Blender Add On Engineer",
            slug = "agency-blender-add-on-engineer",
            division = "Ingénierie & Dev",
            role = "Blender Add On Engineer",
            description = "Blender tooling specialist - Builds Python add-ons, asset validators, exporters, and pipeline automations that turn repetitive DCC work into reliable one-click workflows"
        ),
        AgencyAgent(
            id = "agency-blockchain-security-auditor",
            name = "Blockchain Security Auditor",
            slug = "agency-blockchain-security-auditor",
            division = "Qualité & Tests",
            role = "Blockchain Security Auditor",
            description = "Expert smart contract security auditor specializing in vulnerability detection, formal verification, exploit analysis, and comprehensive audit report writing for DeFi protocols and blockchain applications."
        ),
        AgencyAgent(
            id = "agency-book-co-author",
            name = "Book Co Author",
            slug = "agency-book-co-author",
            division = "Architecture & Systèmes",
            role = "Book Co Author",
            description = "Strategic thought-leadership book collaborator for founders, experts, and operators turning voice notes, fragments, and positioning into structured first-person chapters."
        ),
        AgencyAgent(
            id = "agency-bookkeeper-controller",
            name = "Bookkeeper Controller",
            slug = "agency-bookkeeper-controller",
            division = "Qualité & Tests",
            role = "Bookkeeper Controller",
            description = "Expert bookkeeper and controller specializing in day-to-day accounting operations, financial reconciliations, month-end close processes, and internal controls. Ensures the accuracy, completeness, and timeliness of financial records while maintaining GAAP compliance and audit readiness at all times."
        ),
        AgencyAgent(
            id = "agency-brand-guardian",
            name = "Brand Guardian",
            slug = "agency-brand-guardian",
            division = "Data & AI",
            role = "Brand Guardian",
            description = "Expert brand strategist and guardian specializing in brand identity development, consistency maintenance, and strategic brand positioning"
        ),
        AgencyAgent(
            id = "agency-business-strategist",
            name = "Business Strategist",
            slug = "agency-business-strategist",
            division = "Design & UI/UX",
            role = "Business Strategist",
            description = "Senior management consulting specialist for competitive analysis, market entry strategy, business model design, growth planning, organizational strategy, and strategic decision-making — translating complex market dynamics into clear, actionable strategies that create sustainable competitive advantage"
        ),
        AgencyAgent(
            id = "agency-carousel-growth-engine",
            name = "Carousel Growth Engine",
            slug = "agency-carousel-growth-engine",
            division = "Ingénierie & Dev",
            role = "Carousel Growth Engine",
            description = "Autonomous TikTok and Instagram carousel generation specialist. Analyzes any website URL with Playwright, generates viral 6-slide carousels via Gemini image generation, publishes to TikTok and Instagram through their official APIs or a multi-platform publishing service with auto-added music, fetches analytics, and iteratively improves through a data-driven learning loop."
        ),
        AgencyAgent(
            id = "agency-cartography-designer",
            name = "Cartography Designer",
            slug = "agency-cartography-designer",
            division = "Design & UI/UX",
            role = "Cartography Designer",
            description = "Map aesthetics specialist who designs beautiful, readable, and effective maps — color theory, typography, label placement, basemap selection, and visual hierarchy for both print and web."
        ),
        AgencyAgent(
            id = "agency-change-management-consultant",
            name = "Change Management Consultant",
            slug = "agency-change-management-consultant",
            division = "Design & UI/UX",
            role = "Change Management Consultant",
            description = "Expert change management specialist using ADKAR, Kotter, and Prosci frameworks to guide organizations through technology implementations, restructuring, culture transformation, and M&A integration — managing resistance, building adoption, and ensuring changes stick long after go-live"
        ),
        AgencyAgent(
            id = "agency-chief-financial-officer",
            name = "Chief Financial Officer",
            slug = "agency-chief-financial-officer",
            division = "Ingénierie & Dev",
            role = "Chief Financial Officer",
            description = "Strategic finance executive who governs capital allocation, treasury operations, financial planning, M&A finance, investor relations, and board reporting — translating financial complexity into clear decisions that drive business performance and stakeholder confidence."
        ),
        AgencyAgent(
            id = "agency-chief-of-staff",
            name = "Chief Of Staff",
            slug = "agency-chief-of-staff",
            division = "Ingénierie & Dev",
            role = "Chief Of Staff",
            description = "Master coordinator for founders and executives — filters noise, owns processes, enforces consistency, routes decisions, and positions outputs for impact so the boss can think clearly."
        ),
        AgencyAgent(
            id = "agency-china-e-commerce-operator",
            name = "China E Commerce Operator",
            slug = "agency-china-e-commerce-operator",
            division = "Architecture & Systèmes",
            role = "China E Commerce Operator",
            description = "Expert China e-commerce operations specialist covering Taobao, Tmall, Pinduoduo, and JD ecosystems with deep expertise in product listing optimization, live commerce, store operations, 618/Double 11 campaigns, and cross-platform strategy."
        ),
        AgencyAgent(
            id = "agency-china-market-localization-strategist",
            name = "China Market Localization Strategist",
            slug = "agency-china-market-localization-strategist",
            division = "GIS & Spatial",
            role = "China Market Localization Strategist",
            description = "Full-stack China market localization expert who transforms real-time trend signals into executable go-to-market strategies across Douyin, Xiaohongshu, WeChat, Bilibili, and beyond"
        ),
        AgencyAgent(
            id = "agency-china-network-engineer",
            name = "China Network Engineer",
            slug = "agency-china-network-engineer",
            division = "Ingénierie & Dev",
            role = "China Network Engineer",
            description = "Expert in mainland China''s mainstream enterprise networking stacks — Huawei VRP, H3C Comware, Ruijie RGOS, and Hillstone StoneOS — covering routing, switching, firewalling, NAT, and MLPS 2.0 (等保) compliant border design for domestic deployments."
        ),
        AgencyAgent(
            id = "agency-civil-engineer",
            name = "Civil Engineer",
            slug = "agency-civil-engineer",
            division = "Ingénierie & Dev",
            role = "Civil Engineer",
            description = "Expert civil and structural engineer with global standards coverage — Eurocode, DIN, ACI, AISC, ASCE, AS/NZS, CSA, GB, IS, AIJ, and more. Specializes in structural analysis, geotechnical design, construction documentation, building code compliance, and multi-standard international projects."
        ),
        AgencyAgent(
            id = "agency-clinical-evidence-agent",
            name = "Clinical Evidence Agent",
            slug = "agency-clinical-evidence-agent",
            division = "Qualité & Tests",
            role = "Clinical Evidence Agent",
            description = "Evidence standards and clinical credibility framework for AI agents operating in healthcare contexts. Defines how to distinguish validated from unvalidated clinical claims, how to write for both peer review and investor audiences from the same evidence base, and how to frame clinical decision support without claiming diagnostic authority."
        ),
        AgencyAgent(
            id = "agency-cloud-security-architect",
            name = "Cloud Security Architect",
            slug = "agency-cloud-security-architect",
            division = "Cybersécurité",
            role = "Cloud Security Architect",
            description = "Cloud-native security specialist designing zero trust architectures, implementing defense-in-depth across AWS, Azure, and GCP, and securing infrastructure-as-code pipelines from day one."
        ),
        AgencyAgent(
            id = "agency-cms-developer",
            name = "Cms Developer",
            slug = "agency-cms-developer",
            division = "Ingénierie & Dev",
            role = "Cms Developer",
            description = "Drupal and WordPress specialist for theme development, custom plugins/modules, content architecture, and code-first CMS implementation"
        ),
        AgencyAgent(
            id = "agency-code-reviewer",
            name = "Code Reviewer",
            slug = "agency-code-reviewer",
            division = "Cybersécurité",
            role = "Code Reviewer",
            description = "Expert code reviewer who provides constructive, actionable feedback focused on correctness, maintainability, security, and performance — not style preferences."
        ),
        AgencyAgent(
            id = "agency-codebase-archaeologist",
            name = "Codebase Archaeologist",
            slug = "agency-codebase-archaeologist",
            division = "Qualité & Tests",
            role = "Codebase Archaeologist",
            description = "Multi-session, multi-tool drift detection specialist who audits codebases touched by several AI coding tools (Claude, Cursor, Copilot, Windsurf, etc.) over time, finding silent logic mismatches, dead code, and doc-vs-code divergence that no single session would ever notice on its own."
        ),
        AgencyAgent(
            id = "agency-codebase-onboarding-engineer",
            name = "Codebase Onboarding Engineer",
            slug = "agency-codebase-onboarding-engineer",
            division = "Ingénierie & Dev",
            role = "Codebase Onboarding Engineer",
            description = "Expert developer onboarding specialist who helps new engineers understand unfamiliar codebases fast by reading source code, tracing code paths, and stating only facts grounded in the code."
        ),
        AgencyAgent(
            id = "agency-compliance-auditor",
            name = "Compliance Auditor",
            slug = "agency-compliance-auditor",
            division = "Qualité & Tests",
            role = "Compliance Auditor",
            description = "Expert technical compliance auditor specializing in SOC 2, ISO 27001, HIPAA, and PCI-DSS audits — from readiness assessment through evidence collection to certification."
        ),
        AgencyAgent(
            id = "agency-content-creator",
            name = "Content Creator",
            slug = "agency-content-creator",
            division = "Data & AI",
            role = "Content Creator",
            description = "Expert content strategist and creator for multi-platform campaigns. Develops editorial calendars, creates compelling copy, manages brand storytelling, and optimizes content for engagement across all digital channels."
        ),
        AgencyAgent(
            id = "agency-corporate-training-designer",
            name = "Corporate Training Designer",
            slug = "agency-corporate-training-designer",
            division = "Architecture & Systèmes",
            role = "Corporate Training Designer",
            description = "Expert in enterprise training system design and curriculum development — proficient in training needs analysis, instructional design methodology, blended learning program design, internal trainer development, leadership programs, and training effectiveness evaluation and continuous optimization."
        ),
        AgencyAgent(
            id = "agency-cross-border-e-commerce-specialist",
            name = "Cross Border E Commerce Specialist",
            slug = "agency-cross-border-e-commerce-specialist",
            division = "Product & Project",
            role = "Cross Border E Commerce Specialist",
            description = "Full-funnel cross-border e-commerce strategist covering Amazon, Shopee, Lazada, AliExpress, Temu, and TikTok Shop operations, international logistics and overseas warehousing, compliance and taxation, multilingual listing optimization, brand globalization, and DTC independent site development."
        ),
        AgencyAgent(
            id = "agency-cultural-intelligence-strategist",
            name = "Cultural Intelligence Strategist",
            slug = "agency-cultural-intelligence-strategist",
            division = "GIS & Spatial",
            role = "Cultural Intelligence Strategist",
            description = "CQ specialist that detects invisible exclusion, researches global context, and ensures software resonates authentically across intersectional identities."
        ),
        AgencyAgent(
            id = "agency-customer-service",
            name = "Customer Service",
            slug = "agency-customer-service",
            division = "Design & UI/UX",
            role = "Customer Service",
            description = "Friendly, professional customer service specialist for any industry — handling inquiries, complaints, account support, FAQs, and seamless escalation with warmth, efficiency, and a genuine commitment to customer satisfaction"
        ),
        AgencyAgent(
            id = "agency-customer-success-manager",
            name = "Customer Success Manager",
            slug = "agency-customer-success-manager",
            division = "RH & Organisation",
            role = "Customer Success Manager",
            description = "Strategic customer success specialist for onboarding, health scoring, QBR facilitation, churn prevention, expansion identification, and renewal management — driving net revenue retention by turning customers into long-term partners who achieve measurable outcomes"
        ),
        AgencyAgent(
            id = "agency-data-consolidation-agent",
            name = "Data Consolidation Agent",
            slug = "agency-data-consolidation-agent",
            division = "Data & AI",
            role = "Data Consolidation Agent",
            description = "AI agent that consolidates extracted sales data into live reporting dashboards with territory, rep, and pipeline summaries"
        ),
        AgencyAgent(
            id = "agency-data-engineer",
            name = "Data Engineer",
            slug = "agency-data-engineer",
            division = "Ingénierie & Dev",
            role = "Data Engineer",
            description = "Expert data engineer specializing in building reliable data pipelines, lakehouse architectures, and scalable data infrastructure. Masters ETL/ELT, Apache Spark, dbt, streaming systems, and cloud data platforms to turn raw data into trusted, analytics-ready assets."
        ),
        AgencyAgent(
            id = "agency-data-privacy-officer",
            name = "Data Privacy Officer",
            slug = "agency-data-privacy-officer",
            division = "Design & UI/UX",
            role = "Data Privacy Officer",
            description = "Corporate data privacy specialist and DPO who builds GDPR, CCPA, and global privacy compliance programs — covering data mapping, privacy impact assessments, consent management, breach response, vendor due diligence, and regulatory engagement."
        ),
        AgencyAgent(
            id = "agency-data-visualization-engineer",
            name = "Data Visualization Engineer",
            slug = "agency-data-visualization-engineer",
            division = "Ingénierie & Dev",
            role = "Data Visualization Engineer",
            description = "Expert data visualization engineer — chart-type selection by data and question, perceptually honest encodings, colorblind-safe data palettes, accessible and interactive charts, and rendering large datasets performantly with D3, Vega, and charting libraries."
        ),
        AgencyAgent(
            id = "agency-database-optimizer",
            name = "Database Optimizer",
            slug = "agency-database-optimizer",
            division = "Ingénierie & Dev",
            role = "Database Optimizer",
            description = "Expert database specialist focusing on schema design, query optimization, indexing strategies, and performance tuning for PostgreSQL, MySQL, and modern databases like Supabase and PlanetScale."
        ),
        AgencyAgent(
            id = "agency-database-reliability-engineer",
            name = "Database Reliability Engineer",
            slug = "agency-database-reliability-engineer",
            division = "Ingénierie & Dev",
            role = "Database Reliability Engineer",
            description = "Expert database reliability engineer (DBRE) — high availability and replication, automated failover, backup and point-in-time recovery, zero-downtime online schema migrations, connection pooling, and disaster-recovery drills. Focused on keeping data safe and available, not query tuning."
        ),
        AgencyAgent(
            id = "agency-deal-strategist",
            name = "Deal Strategist",
            slug = "agency-deal-strategist",
            division = "Design & UI/UX",
            role = "Deal Strategist",
            description = "Senior deal strategist specializing in MEDDPICC qualification, competitive positioning, and win planning for complex B2B sales cycles. Scores opportunities, exposes pipeline risk, and builds deal strategies that survive forecast review."
        ),
        AgencyAgent(
            id = "agency-desktop-app-engineer",
            name = "Desktop App Engineer",
            slug = "agency-desktop-app-engineer",
            division = "Ingénierie & Dev",
            role = "Desktop App Engineer",
            description = "Expert desktop application engineer for Electron and Tauri — secure IPC and process isolation, code signing and notarization, auto-update pipelines, native OS integration, and resource-footprint discipline."
        ),
        AgencyAgent(
            id = "agency-developer-advocate",
            name = "Developer Advocate",
            slug = "agency-developer-advocate",
            division = "Ingénierie & Dev",
            role = "Developer Advocate",
            description = "Expert developer advocate specializing in building developer communities, creating compelling technical content, optimizing developer experience (DX), and driving platform adoption through authentic engineering engagement. Bridges product and engineering teams with external developers."
        ),
        AgencyAgent(
            id = "agency-developer-community-builder",
            name = "Developer Community Builder",
            slug = "agency-developer-community-builder",
            division = "Ingénierie & Dev",
            role = "Developer Community Builder",
            description = "Grows and sustains developer communities — Discord servers, GitHub discussions, forums, and contributor programs — turning users into advocates and advocates into contributors."
        ),
        AgencyAgent(
            id = "agency-developer-tooling-engineer",
            name = "Developer Tooling Engineer",
            slug = "agency-developer-tooling-engineer",
            division = "Ingénierie & Dev",
            role = "Developer Tooling Engineer",
            description = "Expert developer-tooling and CLI engineer — building command-line tools and internal developer platforms with great DX: intuitive command design, helpful errors, shell completions, fast startup, cross-platform distribution, and scriptable, composable interfaces."
        ),
        AgencyAgent(
            id = "agency-devops-automator",
            name = "Devops Automator",
            slug = "agency-devops-automator",
            division = "Ingénierie & Dev",
            role = "Devops Automator",
            description = "Expert DevOps engineer specializing in infrastructure automation, CI/CD pipeline development, and cloud operations"
        ),
        AgencyAgent(
            id = "agency-discovery-coach",
            name = "Discovery Coach",
            slug = "agency-discovery-coach",
            division = "Design & UI/UX",
            role = "Discovery Coach",
            description = "Coaches sales teams on elite discovery methodology — question design, current-state mapping, gap quantification, and call structure that surfaces real buying motivation."
        ),
        AgencyAgent(
            id = "agency-document-generator",
            name = "Document Generator",
            slug = "agency-document-generator",
            division = "Design & UI/UX",
            role = "Document Generator",
            description = "Expert document creation specialist who generates professional PDF, PPTX, DOCX, and XLSX files using code-based approaches with proper formatting, charts, and data visualization."
        ),
        AgencyAgent(
            id = "agency-douyin-strategist",
            name = "Douyin Strategist",
            slug = "agency-douyin-strategist",
            division = "Product & Project",
            role = "Douyin Strategist",
            description = "Short-video marketing expert specializing in the Douyin platform, with deep expertise in recommendation algorithm mechanics, viral video planning, livestream commerce workflows, and full-funnel brand growth through content matrix strategies."
        ),
        AgencyAgent(
            id = "agency-drone-reality-mapping-specialist",
            name = "Drone Reality Mapping Specialist",
            slug = "agency-drone-reality-mapping-specialist",
            division = "Data & AI",
            role = "Drone Reality Mapping Specialist",
            description = "Photogrammetry and reality capture expert who processes drone imagery into orthomosaics, digital terrain models, point clouds, and 3D meshes — bridging field capture and GIS-ready products."
        ),
        AgencyAgent(
            id = "agency-drupal-performance-engineer",
            name = "Drupal Performance Engineer",
            slug = "agency-drupal-performance-engineer",
            division = "Qualité & Tests",
            role = "Drupal Performance Engineer",
            description = "Expert Drupal 10/11 performance engineer specializing in Core Web Vitals, render and dynamic page caching, BigPipe, cache tags and contexts, database query and Views optimization, CSS/JS aggregation, responsive images and lazy loading, CDN integration, and opcache/PHP-FPM tuning for fast, audit-passing sites"
        ),
        AgencyAgent(
            id = "agency-drupal-shopping-cart-engineer",
            name = "Drupal Shopping Cart Engineer",
            slug = "agency-drupal-shopping-cart-engineer",
            division = "Ingénierie & Dev",
            role = "Drupal Shopping Cart Engineer",
            description = "Expert Drupal e-commerce engineer specializing in Drupal Commerce for product catalog management, payment gateway integration, checkout workflow design, order management, tax and promotion configuration, and high-reliability storefront delivery on Drupal 10/11"
        ),
        AgencyAgent(
            id = "agency-dx-engineer",
            name = "Dx Engineer",
            slug = "agency-dx-engineer",
            division = "Ingénierie & Dev",
            role = "Dx Engineer",
            description = "Removes every unnecessary step between a developer and their first success — SDK samples, onboarding flows, error messages, and the feedback loops that make products feel like they were built by someone who''s used them."
        ),
        AgencyAgent(
            id = "agency-economy-designer",
            name = "Economy Designer",
            slug = "agency-economy-designer",
            division = "Architecture & Systèmes",
            role = "Economy Designer",
            description = "Virtual economy architect - Masters currency systems, sources and sinks, monetization modeling, inflation control, and data-driven economic balancing for live games"
        ),
        AgencyAgent(
            id = "agency-electrical-engineer",
            name = "Electrical Engineer",
            slug = "agency-electrical-engineer",
            division = "Ingénierie & Dev",
            role = "Electrical Engineer",
            description = "Power systems engineer for telecom sites, buildings and infrastructure projects. Sizes loads, cables, batteries, rectifiers, generators and solar; designs earthing and lightning protection; reviews drawings, BOQs and commissioning records; and turns electrical requirements into software calculation rules and validation checks with explicit professional-review boundaries."
        ),
        AgencyAgent(
            id = "agency-email-intelligence-engineer",
            name = "Email Intelligence Engineer",
            slug = "agency-email-intelligence-engineer",
            division = "Ingénierie & Dev",
            role = "Email Intelligence Engineer",
            description = "Expert in extracting structured, reasoning-ready data from raw email threads for AI agents and automation systems"
        ),
        AgencyAgent(
            id = "agency-email-marketing-strategist",
            name = "Email Marketing Strategist",
            slug = "agency-email-marketing-strategist",
            division = "Qualité & Tests",
            role = "Email Marketing Strategist",
            description = "Expert email marketing strategist for CRM-driven campaigns, lifecycle automation, segmentation architecture, and deliverability. Designs sequences (welcome, nurture, reactivation, win-back, review, referral) grounded in 2025-2026 benchmarks, AI-driven personalization, and post-Apple MPP measurement."
        ),
        AgencyAgent(
            id = "agency-embedded-firmware-engineer",
            name = "Embedded Firmware Engineer",
            slug = "agency-embedded-firmware-engineer",
            division = "Ingénierie & Dev",
            role = "Embedded Firmware Engineer",
            description = "Specialist in bare-metal and RTOS firmware - ESP32/ESP-IDF, PlatformIO, Arduino, ARM Cortex-M, STM32 HAL/LL, Nordic nRF5/nRF Connect SDK, FreeRTOS, Zephyr"
        ),
        AgencyAgent(
            id = "agency-esg-sustainability-officer",
            name = "Esg Sustainability Officer",
            slug = "agency-esg-sustainability-officer",
            division = "Design & UI/UX",
            role = "Esg Sustainability Officer",
            description = "Corporate sustainability strategist and ESG reporting specialist who builds environmental, social, and governance programs, manages disclosures, drives decarbonization initiatives, and aligns business strategy with stakeholder and regulatory expectations."
        ),
        AgencyAgent(
            id = "agency-evidence-collector",
            name = "Evidence Collector",
            slug = "agency-evidence-collector",
            division = "Qualité & Tests",
            role = "Evidence Collector",
            description = "Screenshot-obsessed, fantasy-allergic QA specialist - Reports reproducible issues with evidence and marks untested scope honestly"
        ),
        AgencyAgent(
            id = "agency-executive-summary-generator",
            name = "Executive Summary Generator",
            slug = "agency-executive-summary-generator",
            division = "Qualité & Tests",
            role = "Executive Summary Generator",
            description = "Consultant-grade AI specialist trained to think and communicate like a senior strategy consultant. Transforms complex business inputs into concise, actionable executive summaries using McKinsey SCQA, BCG Pyramid Principle, and Bain frameworks for C-suite decision-makers."
        ),
        AgencyAgent(
            id = "agency-experiment-tracker",
            name = "Experiment Tracker",
            slug = "agency-experiment-tracker",
            division = "Qualité & Tests",
            role = "Experiment Tracker",
            description = "Expert project manager specializing in experiment design, execution tracking, and data-driven decision making. Focused on managing A/B tests, feature experiments, and hypothesis validation through systematic experimentation and rigorous analysis."
        ),
        AgencyAgent(
            id = "agency-fedramp-rmf-compliance-engineer",
            name = "Fedramp Rmf Compliance Engineer",
            slug = "agency-fedramp-rmf-compliance-engineer",
            division = "Cybersécurité",
            role = "Fedramp Rmf Compliance Engineer",
            description = "Expert FedRAMP and NIST Risk Management Framework compliance engineer specializing in both FedRAMP authorization pathways — the traditional Rev5 path (NIST 800-53 Rev 5 control implementation, System Security Plans, 3PAO assessment, agency authorization) and the modernized FedRAMP 20x path (Key Security Indicators, automated machine-readable validation, compliance-as-code) — plus the ATO process, continuous monitoring (ConMon), POA&M management, FIPS 199 categorization, authorization boundary diagrams, OSCAL machine-readable packages, and cloud security compliance for government and regulated industries"
        ),
        AgencyAgent(
            id = "agency-feedback-synthesizer",
            name = "Feedback Synthesizer",
            slug = "agency-feedback-synthesizer",
            division = "Product & Project",
            role = "Feedback Synthesizer",
            description = "Expert in collecting, analyzing, and synthesizing user feedback from multiple channels to extract actionable product insights. Transforms qualitative feedback into quantitative priorities and strategic recommendations."
        ),
        AgencyAgent(
            id = "agency-feishu-integration-developer",
            name = "Feishu Integration Developer",
            slug = "agency-feishu-integration-developer",
            division = "Ingénierie & Dev",
            role = "Feishu Integration Developer",
            description = "Full-stack integration expert specializing in the Feishu (Lark) Open Platform — proficient in Feishu bots, mini programs, approval workflows, Bitable (multidimensional spreadsheets), interactive message cards, Webhooks, SSO authentication, and workflow automation, building enterprise-grade collaboration and automation solutions within the Feishu ecosystem."
        ),
        AgencyAgent(
            id = "agency-filament-optimization-specialist",
            name = "Filament Optimization Specialist",
            slug = "agency-filament-optimization-specialist",
            division = "Ingénierie & Dev",
            role = "Filament Optimization Specialist",
            description = "Expert in restructuring and optimizing Filament PHP admin interfaces for maximum usability and efficiency. Focuses on impactful structural changes — not just cosmetic tweaks."
        ),
        AgencyAgent(
            id = "agency-finance-tracker",
            name = "Finance Tracker",
            slug = "agency-finance-tracker",
            division = "Data & AI",
            role = "Finance Tracker",
            description = "Expert financial analyst and controller specializing in financial planning, budget management, and business performance analysis. Maintains financial health, optimizes cash flow, and provides strategic financial insights for business growth."
        ),
        AgencyAgent(
            id = "agency-financial-analyst",
            name = "Financial Analyst",
            slug = "agency-financial-analyst",
            division = "Data & AI",
            role = "Financial Analyst",
            description = "Expert financial analyst specializing in financial modeling, forecasting, scenario analysis, and data-driven decision support. Transforms raw financial data into actionable business intelligence that drives strategic planning, investment decisions, and operational optimization."
        ),
        AgencyAgent(
            id = "agency-finops-engineer",
            name = "Finops Engineer",
            slug = "agency-finops-engineer",
            division = "Ingénierie & Dev",
            role = "Finops Engineer",
            description = "Expert cloud cost engineer for AWS/GCP/Azure — cost allocation and tagging, rightsizing, commitment planning (reserved instances/savings plans), egress and storage optimization, and unit-economics dashboards that tie spend to business value."
        ),
        AgencyAgent(
            id = "agency-focus-music-architect",
            name = "Focus Music Architect",
            slug = "agency-focus-music-architect",
            division = "Ingénierie & Dev",
            role = "Focus Music Architect",
            description = "Instrumental focus music specialist and neuroacoustic prompt engineer — crafts high-yield prompts, soundscape architectures, BPM curves, and binaural layers for deep cognitive flow and generative audio models."
        ),
        AgencyAgent(
            id = "agency-fp-a-analyst",
            name = "Fp A Analyst",
            slug = "agency-fp-a-analyst",
            division = "Game Development",
            role = "Fp A Analyst",
            description = "Expert Financial Planning & Analysis (FP&A) analyst specializing in budgeting, variance analysis, financial planning, rolling forecasts, and strategic decision support. Bridges the gap between the numbers and the business narrative to drive operational performance and strategic resource allocation."
        ),
        AgencyAgent(
            id = "agency-french-consulting-market-navigator",
            name = "French Consulting Market Navigator",
            slug = "agency-french-consulting-market-navigator",
            division = "Architecture & Systèmes",
            role = "French Consulting Market Navigator",
            description = "Navigate the French ESN/SI freelance ecosystem — margin models, platform mechanics (Malt, collective.work), portage salarial, rate positioning, and payment cycle realities"
        ),
        AgencyAgent(
            id = "agency-frontend-developer",
            name = "Frontend Developer",
            slug = "agency-frontend-developer",
            division = "Ingénierie & Dev",
            role = "Frontend Developer",
            description = "Expert frontend developer specializing in modern web technologies, React/Vue/Angular frameworks, UI implementation, and performance optimization"
        ),
        AgencyAgent(
            id = "agency-game-audio-engineer",
            name = "Game Audio Engineer",
            slug = "agency-game-audio-engineer",
            division = "Ingénierie & Dev",
            role = "Game Audio Engineer",
            description = "Interactive audio specialist - Masters FMOD/Wwise integration, adaptive music systems, spatial audio, and audio performance budgeting across all game engines"
        ),
        AgencyAgent(
            id = "agency-game-designer",
            name = "Game Designer",
            slug = "agency-game-designer",
            division = "Architecture & Systèmes",
            role = "Game Designer",
            description = "Systems and mechanics architect - Masters GDD authorship, player psychology, economy balancing, and gameplay loop design across all engines and genres"
        ),
        AgencyAgent(
            id = "agency-gaussdb-expert-engineer",
            name = "Gaussdb Expert Engineer",
            slug = "agency-gaussdb-expert-engineer",
            division = "Ingénierie & Dev",
            role = "Gaussdb Expert Engineer",
            description = "Expert database specialist focusing on GaussDB OLTP — Huawei''s self-developed enterprise-grade relational database (NOT GaussDB(DWS) OLAP, NOT GaussDB(for openGauss) cloud service, NOT GaussDB(for MySQL)). Covers schema design, distributed table design, query optimization, indexing, Ustore engine, and performance tuning for both distributed and centralized deployments."
        ),
        AgencyAgent(
            id = "agency-geoai-ml-engineer",
            name = "Geoai Ml Engineer",
            slug = "agency-geoai-ml-engineer",
            division = "Ingénierie & Dev",
            role = "Geoai Ml Engineer",
            description = "Geospatial machine learning specialist who builds models for feature extraction, object detection, image segmentation, and land cover classification from satellite and aerial imagery."
        ),
        AgencyAgent(
            id = "agency-geographer",
            name = "Geographer",
            slug = "agency-geographer",
            division = "Architecture & Systèmes",
            role = "Geographer",
            description = "Expert in physical and human geography, climate systems, cartography, and spatial analysis — builds geographically coherent worlds where terrain, climate, resources, and settlement patterns make scientific sense"
        ),
        AgencyAgent(
            id = "agency-geoprocessing-specialist",
            name = "Geoprocessing Specialist",
            slug = "agency-geoprocessing-specialist",
            division = "Design & UI/UX",
            role = "Geoprocessing Specialist",
            description = "ArcPy and Python toolbox expert who automates spatial workflows — builds .pyt toolboxes, Model Builder processes, batch geoprocessing automation, and custom analysis scripts for ArcGIS Pro."
        ),
        AgencyAgent(
            id = "agency-gis-analyst",
            name = "Gis Analyst",
            slug = "agency-gis-analyst",
            division = "Data & AI",
            role = "Gis Analyst",
            description = "Day-to-day GIS operator who creates maps, manages layers, performs spatial queries, and maintains geospatial data integrity across desktop and web environments."
        ),
        AgencyAgent(
            id = "agency-gis-qa-engineer",
            name = "Gis Qa Engineer",
            slug = "agency-gis-qa-engineer",
            division = "Qualité & Tests",
            role = "Gis Qa Engineer",
            description = "Quality assurance specialist who validates geospatial data integrity — topology checks, metadata audits, CRS consistency, accuracy assessment, and compliance verification."
        ),
        AgencyAgent(
            id = "agency-git-workflow-master",
            name = "Git Workflow Master",
            slug = "agency-git-workflow-master",
            division = "Product & Project",
            role = "Git Workflow Master",
            description = "Expert in Git workflows, branching strategies, and version control best practices including conventional commits, rebasing, worktrees, and CI-friendly branch management."
        ),
        AgencyAgent(
            id = "agency-global-podcast-strategist",
            name = "Global Podcast Strategist",
            slug = "agency-global-podcast-strategist",
            division = "GIS & Spatial",
            role = "Global Podcast Strategist",
            description = "Expert podcast growth specialist focused on show positioning, audience development, content strategy, and monetisation. Transforms raw ideas into authoritative audio brands that compound listeners and revenue over time on Spotify, Apple Podcasts, and YouTube."
        ),
        AgencyAgent(
            id = "agency-godot-gameplay-scripter",
            name = "Godot Gameplay Scripter",
            slug = "agency-godot-gameplay-scripter",
            division = "Architecture & Systèmes",
            role = "Godot Gameplay Scripter",
            description = "Composition and signal integrity specialist - Masters GDScript 2.0, C# integration, node-based architecture, and type-safe signal design for Godot 4 projects"
        ),
        AgencyAgent(
            id = "agency-godot-multiplayer-engineer",
            name = "Godot Multiplayer Engineer",
            slug = "agency-godot-multiplayer-engineer",
            division = "Ingénierie & Dev",
            role = "Godot Multiplayer Engineer",
            description = "Godot 4 networking specialist - Masters the MultiplayerAPI, scene replication, ENet/WebRTC transport, RPCs, and authority models for real-time multiplayer games"
        ),
        AgencyAgent(
            id = "agency-godot-shader-developer",
            name = "Godot Shader Developer",
            slug = "agency-godot-shader-developer",
            division = "Ingénierie & Dev",
            role = "Godot Shader Developer",
            description = "Godot 4 visual effects specialist - Masters the Godot Shading Language (GLSL-like), VisualShader editor, CanvasItem and Spatial shaders, post-processing, and performance optimization for 2D/3D effects"
        ),
        AgencyAgent(
            id = "agency-government-digital-presales-consultant",
            name = "Government Digital Presales Consultant",
            slug = "agency-government-digital-presales-consultant",
            division = "Design & UI/UX",
            role = "Government Digital Presales Consultant",
            description = "Presales expert for China''s government digital transformation market (ToG), proficient in policy interpretation, solution design, bid document preparation, POC validation, compliance requirements (classified protection/cryptographic assessment/Xinchuang domestic IT), and stakeholder management — helping technical teams efficiently win government IT projects."
        ),
        AgencyAgent(
            id = "agency-grant-writer",
            name = "Grant Writer",
            slug = "agency-grant-writer",
            division = "Design & UI/UX",
            role = "Grant Writer",
            description = "Expert grant writing specialist for nonprofits, research institutions, and social enterprises — covering prospect research, letter of inquiry writing, full proposal development, budget narratives, federal and foundation grants, and post-award reporting to maximize funding success"
        ),
        AgencyAgent(
            id = "agency-growth-hacker",
            name = "Growth Hacker",
            slug = "agency-growth-hacker",
            division = "Ingénierie & Dev",
            role = "Growth Hacker",
            description = "Expert growth strategist specializing in rapid user acquisition through data-driven experimentation. Develops viral loops, optimizes conversion funnels, and finds scalable growth channels for exponential business growth."
        ),
        AgencyAgent(
            id = "agency-healthcare-customer-service",
            name = "Healthcare Customer Service",
            slug = "agency-healthcare-customer-service",
            division = "Design & UI/UX",
            role = "Healthcare Customer Service",
            description = "Empathetic healthcare customer service specialist for patient support, billing inquiries, appointment management, insurance questions, complaint resolution, and seamless escalation to clinical or administrative staff"
        ),
        AgencyAgent(
            id = "agency-healthcare-innovation-strategist",
            name = "Healthcare Innovation Strategist",
            slug = "agency-healthcare-innovation-strategist",
            division = "Ingénierie & Dev",
            role = "Healthcare Innovation Strategist",
            description = "Strategic narrative architect for healthcare founders operating at the intersection of clinical credibility, healthcare finance, and complex deployment contexts. Maintains narrative coherence across investor, regulatory, sovereign, and clinical audiences. Built for founders who need to translate complex clinical and financial realities into language that moves capital, changes policy, and builds trust with doctors and patients simultaneously."
        ),
        AgencyAgent(
            id = "agency-healthcare-marketing-compliance-specialist",
            name = "Healthcare Marketing Compliance Specialist",
            slug = "agency-healthcare-marketing-compliance-specialist",
            division = "DevOps & Cloud",
            role = "Healthcare Marketing Compliance Specialist",
            description = "Expert in healthcare marketing compliance in China, proficient in the Advertising Law, Medical Advertisement Management Measures, Drug Administration Law, and related regulations — covering pharmaceuticals, medical devices, medical aesthetics, health supplements, and internet healthcare across content review, risk control, platform rule interpretation, and patient privacy protection, helping enterprises conduct effective health marketing within legal boundaries."
        ),
        AgencyAgent(
            id = "agency-historian",
            name = "Historian",
            slug = "agency-historian",
            division = "Data & AI",
            role = "Historian",
            description = "Expert in historical analysis, periodization, material culture, and historiography — validates historical coherence and enriches settings with authentic period detail grounded in primary and secondary sources"
        ),
        AgencyAgent(
            id = "agency-hospitality-guest-services",
            name = "Hospitality Guest Services",
            slug = "agency-hospitality-guest-services",
            division = "Data & AI",
            role = "Hospitality Guest Services",
            description = "Comprehensive hospitality guest services specialist for hotels, resorts, restaurants, and event venues — covering reservations, check-in/check-out, concierge services, guest complaint resolution, loyalty program management, and post-stay follow-up to deliver exceptional guest experiences that drive loyalty and revenue"
        ),
        AgencyAgent(
            id = "agency-hr-onboarding",
            name = "Hr Onboarding",
            slug = "agency-hr-onboarding",
            division = "Data & AI",
            role = "Hr Onboarding",
            description = "Comprehensive HR onboarding specialist for employee orientation, documentation management, compliance tracking, benefits enrollment, culture integration, and new hire support — delivering a seamless first-day-to-first-year experience that drives retention and productivity"
        ),
        AgencyAgent(
            id = "agency-identity-access-engineer",
            name = "Identity Access Engineer",
            slug = "agency-identity-access-engineer",
            division = "Ingénierie & Dev",
            role = "Identity Access Engineer",
            description = "Expert identity engineer for OAuth 2.0/OIDC flows, enterprise SSO (SAML/OIDC) and SCIM provisioning, passkeys/WebAuthn, session architecture, and multi-tenant authorization with RBAC/ABAC."
        ),
        AgencyAgent(
            id = "agency-identity-graph-operator",
            name = "Identity Graph Operator",
            slug = "agency-identity-graph-operator",
            division = "Architecture & Systèmes",
            role = "Identity Graph Operator",
            description = "Operates a shared identity graph that multiple AI agents resolve against. Ensures every agent in a multi-agent system gets the same canonical answer for \"who is this entity?\" - deterministically, even under concurrent writes."
        ),
        AgencyAgent(
            id = "agency-image-prompt-engineer",
            name = "Image Prompt Engineer",
            slug = "agency-image-prompt-engineer",
            division = "Qualité & Tests",
            role = "Image Prompt Engineer",
            description = "Expert photography prompt engineer specializing in crafting detailed, evocative prompts for AI image generation. Masters the art of translating visual concepts into precise language that produces stunning, professional-quality photography through generative AI tools."
        ),
        AgencyAgent(
            id = "agency-incident-responder",
            name = "Incident Responder",
            slug = "agency-incident-responder",
            division = "Cybersécurité",
            role = "Incident Responder",
            description = "Digital forensics and incident response specialist who leads breach investigations, contains active threats, coordinates crisis response, and writes post-mortems that prevent recurrence."
        ),
        AgencyAgent(
            id = "agency-incident-response-commander",
            name = "Incident Response Commander",
            slug = "agency-incident-response-commander",
            division = "Cybersécurité",
            role = "Incident Response Commander",
            description = "Expert incident commander specializing in production incident management, structured response coordination, post-mortem facilitation, SLO/SLI tracking, and on-call process design for reliable engineering organizations."
        ),
        AgencyAgent(
            id = "agency-inclusive-visuals-specialist",
            name = "Inclusive Visuals Specialist",
            slug = "agency-inclusive-visuals-specialist",
            division = "Architecture & Systèmes",
            role = "Inclusive Visuals Specialist",
            description = "Representation expert who defeats systemic AI biases to generate culturally accurate, affirming, and non-stereotypical images and video."
        ),
        AgencyAgent(
            id = "agency-infrastructure-maintainer",
            name = "Infrastructure Maintainer",
            slug = "agency-infrastructure-maintainer",
            division = "Cybersécurité",
            role = "Infrastructure Maintainer",
            description = "Expert infrastructure specialist focused on system reliability, performance optimization, and technical operations management. Maintains robust, scalable infrastructure supporting business operations with security, performance, and cost efficiency."
        ),
        AgencyAgent(
            id = "agency-instagram-curator",
            name = "Instagram Curator",
            slug = "agency-instagram-curator",
            division = "Design & UI/UX",
            role = "Instagram Curator",
            description = "Expert Instagram marketing specialist focused on visual storytelling, community building, and multi-format content optimization. Masters aesthetic development and drives meaningful engagement."
        ),
        AgencyAgent(
            id = "agency-internationalization-engineer",
            name = "Internationalization Engineer",
            slug = "agency-internationalization-engineer",
            division = "Qualité & Tests",
            role = "Internationalization Engineer",
            description = "Expert i18n engineer for ICU MessageFormat, CLDR plural rules, RTL and bidirectional layouts, locale-aware date/number/currency formatting, string extraction pipelines, and pseudo-localization testing."
        ),
        AgencyAgent(
            id = "agency-investment-researcher",
            name = "Investment Researcher",
            slug = "agency-investment-researcher",
            division = "Design & UI/UX",
            role = "Investment Researcher",
            description = "Expert investment researcher specializing in market research, due diligence, portfolio analysis, and asset valuation. Conducts rigorous fundamental and quantitative analysis to identify investment opportunities, assess risks, and support data-driven portfolio decisions across public equities, private markets, and alternative assets."
        ),
        AgencyAgent(
            id = "agency-iot-fleet-engineer",
            name = "Iot Fleet Engineer",
            slug = "agency-iot-fleet-engineer",
            division = "Ingénierie & Dev",
            role = "Iot Fleet Engineer",
            description = "Expert IoT and edge fleet engineer — device provisioning and identity, MQTT/telemetry pipelines, staged over-the-air (OTA) firmware updates with rollback, edge compute, and observability across fleets of unreliable, intermittently-connected devices."
        ),
        AgencyAgent(
            id = "agency-it-service-manager",
            name = "It Service Manager",
            slug = "agency-it-service-manager",
            division = "Cybersécurité",
            role = "It Service Manager",
            description = "Expert IT service management specialist using ITIL 4 framework for service catalog design, incident and problem management, change control, SLA governance, CMDB maintenance, and continual service improvement — ensuring IT delivers reliable, measurable business value across any organization size"
        ),
        AgencyAgent(
            id = "agency-jira-workflow-steward",
            name = "Jira Workflow Steward",
            slug = "agency-jira-workflow-steward",
            division = "Product & Project",
            role = "Jira Workflow Steward",
            description = "Expert delivery operations specialist who enforces Jira-linked Git workflows, traceable commits, structured pull requests, and release-safe branch strategy across software teams."
        ),
        AgencyAgent(
            id = "agency-knowledge-graph-engineer",
            name = "Knowledge Graph Engineer",
            slug = "agency-knowledge-graph-engineer",
            division = "Ingénierie & Dev",
            role = "Knowledge Graph Engineer",
            description = "Structures information and capabilities into interconnected nodes (entities) and edges (relationships) — enabling dynamic context navigation, modular competency chaining, lower token costs, and hallucination reduction."
        ),
        AgencyAgent(
            id = "agency-korean-business-navigator",
            name = "Korean Business Navigator",
            slug = "agency-korean-business-navigator",
            division = "Stratégie & Ventes",
            role = "Korean Business Navigator",
            description = "Korean business culture for foreign professionals — 품의 decision process, nunchi reading, KakaoTalk business etiquette, hierarchy navigation, and relationship-first deal mechanics"
        ),
        AgencyAgent(
            id = "agency-kuaishou-strategist",
            name = "Kuaishou Strategist",
            slug = "agency-kuaishou-strategist",
            division = "Ingénierie & Dev",
            role = "Kuaishou Strategist",
            description = "Expert Kuaishou marketing strategist specializing in short-video content for China''s lower-tier city markets, live commerce operations, community trust building, and grassroots audience growth on 快手."
        ),
        AgencyAgent(
            id = "agency-language-translator",
            name = "Language Translator",
            slug = "agency-language-translator",
            division = "Design & UI/UX",
            role = "Language Translator",
            description = "Real-time Spanish ↔ English translation specialist with cultural context, regional dialect awareness, travel phrase guidance, and tone-appropriate communication for everyday, business, and emergency situations"
        ),
        AgencyAgent(
            id = "agency-legal-billing-time-tracking",
            name = "Legal Billing Time Tracking",
            slug = "agency-legal-billing-time-tracking",
            division = "Ingénierie & Dev",
            role = "Legal Billing Time Tracking",
            description = "Comprehensive legal billing and time tracking specialist for accurate time capture, invoice generation, billing narrative writing, collections management, trust account compliance, and billing analysis — maximizing revenue recovery while maintaining client relationships and ethical compliance across any firm size or billing model"
        ),
        AgencyAgent(
            id = "agency-legal-client-intake",
            name = "Legal Client Intake",
            slug = "agency-legal-client-intake",
            division = "Conformité & Légal",
            role = "Legal Client Intake",
            description = "Comprehensive legal client intake specialist for qualifying prospects, collecting case information, scheduling consultations, managing conflict checks, and delivering attorney-ready intake summaries across any practice area and firm size"
        ),
        AgencyAgent(
            id = "agency-legal-compliance-checker",
            name = "Legal Compliance Checker",
            slug = "agency-legal-compliance-checker",
            division = "Qualité & Tests",
            role = "Legal Compliance Checker",
            description = "Expert legal and compliance specialist ensuring business operations, data handling, and content creation comply with relevant laws, regulations, and industry standards across multiple jurisdictions."
        ),
        AgencyAgent(
            id = "agency-legal-document-review",
            name = "Legal Document Review",
            slug = "agency-legal-document-review",
            division = "Conformité & Légal",
            role = "Legal Document Review",
            description = "Comprehensive legal document review specialist for contracts, litigation documents, and real estate agreements — summarizing documents, flagging risk clauses, comparing contract versions, and checking compliance across any law firm size or practice area"
        ),
        AgencyAgent(
            id = "agency-level-designer",
            name = "Level Designer",
            slug = "agency-level-designer",
            division = "Architecture & Systèmes",
            role = "Level Designer",
            description = "Spatial storytelling and flow specialist - Masters layout theory, pacing architecture, encounter design, and environmental narrative across all game engines"
        ),
        AgencyAgent(
            id = "agency-linkedin-content-creator",
            name = "Linkedin Content Creator",
            slug = "agency-linkedin-content-creator",
            division = "Ingénierie & Dev",
            role = "Linkedin Content Creator",
            description = "Expert LinkedIn content strategist focused on thought leadership, personal brand building, and high-engagement professional content. Masters LinkedIn''s algorithm and culture to drive inbound opportunities for founders, job seekers, developers, and anyone building a professional presence."
        ),
        AgencyAgent(
            id = "agency-livestream-commerce-coach",
            name = "Livestream Commerce Coach",
            slug = "agency-livestream-commerce-coach",
            division = "Design & UI/UX",
            role = "Livestream Commerce Coach",
            description = "Veteran livestream e-commerce coach specializing in host training and live room operations across Douyin, Kuaishou, Taobao Live, and Channels, covering script design, product sequencing, paid-vs-organic traffic balancing, conversion closing techniques, and real-time data-driven optimization."
        ),
        AgencyAgent(
            id = "agency-llm-post-training-engineer",
            name = "Llm Post Training Engineer",
            slug = "agency-llm-post-training-engineer",
            division = "Qualité & Tests",
            role = "Llm Post Training Engineer",
            description = "Evidence-driven owner for SFT, preference optimization, RLHF/RLVR, MoE post-training, and the release gates that turn a checkpoint into a defensible model change."
        ),
        AgencyAgent(
            id = "agency-loan-officer-assistant",
            name = "Loan Officer Assistant",
            slug = "agency-loan-officer-assistant",
            division = "Conformité & Légal",
            role = "Loan Officer Assistant",
            description = "Comprehensive loan officer assistant for mortgage and lending professionals — covering borrower intake, pre-qualification, document collection, pipeline management, compliance tracking, rate quoting, and closing coordination across residential, commercial, and consumer lending"
        ),
        AgencyAgent(
            id = "agency-lsp-index-engineer",
            name = "Lsp Index Engineer",
            slug = "agency-lsp-index-engineer",
            division = "Ingénierie & Dev",
            role = "Lsp Index Engineer",
            description = "Language Server Protocol specialist building unified code intelligence systems through LSP client orchestration and semantic indexing"
        ),
        AgencyAgent(
            id = "agency-m-a-integration-manager",
            name = "M A Integration Manager",
            slug = "agency-m-a-integration-manager",
            division = "Design & UI/UX",
            role = "M A Integration Manager",
            description = "Mergers and acquisitions integration specialist who designs and executes post-merger integration programs — covering Day 1 readiness, 100-day planning, synergy tracking, cultural integration, functional workstream coordination, and transition service agreement management."
        ),
        AgencyAgent(
            id = "agency-macos-spatial-metal-engineer",
            name = "Macos Spatial Metal Engineer",
            slug = "agency-macos-spatial-metal-engineer",
            division = "Ingénierie & Dev",
            role = "Macos Spatial Metal Engineer",
            description = "Native Swift and Metal specialist building high-performance 3D rendering systems and spatial computing experiences for macOS and Vision Pro"
        ),
        AgencyAgent(
            id = "agency-master-plan-architect",
            name = "Master Plan Architect",
            slug = "agency-master-plan-architect",
            division = "Architecture & Systèmes",
            role = "Master Plan Architect",
            description = "Master planning architect, technical educator, and ruthless plan critic who specializes in deep architectural teaching, Red Teaming / risk critique, and crafting comprehensive Implementation Plans in Markdown with ZERO code execution."
        ),
        AgencyAgent(
            id = "agency-mcp-builder",
            name = "Mcp Builder",
            slug = "agency-mcp-builder",
            division = "Qualité & Tests",
            role = "Mcp Builder",
            description = "Expert Model Context Protocol developer who designs, builds, and tests MCP servers that extend AI agent capabilities with custom tools, resources, and prompts."
        ),
        AgencyAgent(
            id = "agency-medical-billing-coding-specialist",
            name = "Medical Billing Coding Specialist",
            slug = "agency-medical-billing-coding-specialist",
            division = "Qualité & Tests",
            role = "Medical Billing Coding Specialist",
            description = "Expert medical billing and coding specialist for ICD-10-CM/PCS, CPT, and HCPCS coding, claim submission, denial management, revenue cycle optimization, compliance auditing, and payer contract analysis — maximizing clean claim rates and revenue recovery for healthcare providers of all sizes"
        ),
        AgencyAgent(
            id = "agency-meeting-notes-specialist",
            name = "Meeting Notes Specialist",
            slug = "agency-meeting-notes-specialist",
            division = "Ingénierie & Dev",
            role = "Meeting Notes Specialist",
            description = "Extract structured decisions, action items, and open questions from meeting transcripts or rough notes into a clean 4-section summary."
        ),
        AgencyAgent(
            id = "agency-minimal-change-engineer",
            name = "Minimal Change Engineer",
            slug = "agency-minimal-change-engineer",
            division = "Ingénierie & Dev",
            role = "Minimal Change Engineer",
            description = "Engineering specialist focused on minimum-viable diffs — fixes only what was asked, refuses scope creep, prefers three similar lines over a premature abstraction. The discipline that prevents bug-fix PRs from becoming refactor avalanches."
        ),
        AgencyAgent(
            id = "agency-mobile-app-builder",
            name = "Mobile App Builder",
            slug = "agency-mobile-app-builder",
            division = "Ingénierie & Dev",
            role = "Mobile App Builder",
            description = "Specialized mobile application developer with expertise in native iOS/Android development and cross-platform frameworks"
        ),
        AgencyAgent(
            id = "agency-mobile-release-engineer",
            name = "Mobile Release Engineer",
            slug = "agency-mobile-release-engineer",
            division = "Ingénierie & Dev",
            role = "Mobile Release Engineer",
            description = "Expert mobile release and distribution engineer for iOS and Android — code signing, provisioning, fastlane pipelines, App Store Connect and Play Console submission, phased rollouts, and crash-triaged release health."
        ),
        AgencyAgent(
            id = "agency-model-qa-specialist",
            name = "Model Qa Specialist",
            slug = "agency-model-qa-specialist",
            division = "Qualité & Tests",
            role = "Model Qa Specialist",
            description = "Independent model QA expert who audits ML and statistical models end-to-end - from documentation review and data reconstruction to replication, calibration testing, interpretability analysis, performance monitoring, and audit-grade reporting."
        ),
        AgencyAgent(
            id = "agency-multi-agent-systems-architect",
            name = "Multi Agent Systems Architect",
            slug = "agency-multi-agent-systems-architect",
            division = "Ingénierie & Dev",
            role = "Multi Agent Systems Architect",
            description = "Systems architect specializing in the design, coordination, and governance of multi-agent AI pipelines — covering topology selection, context management, inter-agent trust, failure recovery, human-in-the-loop gating, and observability for production-grade agent systems."
        ),
        AgencyAgent(
            id = "agency-multi-platform-publisher",
            name = "Multi Platform Publisher",
            slug = "agency-multi-platform-publisher",
            division = "Data & AI",
            role = "Multi Platform Publisher",
            description = "Expert orchestrator for one-click Chinese blog publishing. Routes a single article to 知乎 / 小红书 / CSDN / B站 / 公众号 / 掘金 via Wechatsync (main channel) with xhs-mcp and biliup as specialized fallbacks. Handles per-platform content adaptation, draft-first publishing, rate control, and risk-avoidance. Does NOT auto-publish — always stops at draft for human review."
        ),
        AgencyAgent(
            id = "agency-narrative-designer",
            name = "Narrative Designer",
            slug = "agency-narrative-designer",
            division = "Architecture & Systèmes",
            role = "Narrative Designer",
            description = "Story systems and dialogue architect - Masters GDD-aligned narrative design, branching dialogue, lore architecture, and environmental storytelling across all game engines"
        ),
        AgencyAgent(
            id = "agency-narratologist",
            name = "Narratologist",
            slug = "agency-narratologist",
            division = "GIS & Spatial",
            role = "Narratologist",
            description = "Expert in narrative theory, story structure, character arcs, and literary analysis — grounds advice in established frameworks from Propp to Campbell to modern narratology"
        ),
        AgencyAgent(
            id = "agency-network-engineer",
            name = "Network Engineer",
            slug = "agency-network-engineer",
            division = "Ingénierie & Dev",
            role = "Network Engineer",
            description = "Expert network engineer for Cisco IOS/IOS-XE, Cisco ASA/FTD, Juniper Junos, and Palo Alto PAN-OS routing, switching, firewalling, and troubleshooting."
        ),
        AgencyAgent(
            id = "agency-offer-lead-gen-strategist",
            name = "Offer Lead Gen Strategist",
            slug = "agency-offer-lead-gen-strategist",
            division = "Architecture & Systèmes",
            role = "Offer Lead Gen Strategist",
            description = "Top-of-funnel architect who designs irresistible offers and lead magnets that attract qualified buyers at scale. Specializes in value-equation offer construction, lead magnet typology, multi-channel lead generation, and compounding reach through customers, employees, agencies, and affiliates."
        ),
        AgencyAgent(
            id = "agency-operations-manager",
            name = "Operations Manager",
            slug = "agency-operations-manager",
            division = "Architecture & Systèmes",
            role = "Operations Manager",
            description = "Business operations specialist who applies Lean, Six Sigma, and systems thinking to process mapping, capacity planning, KPI governance, vendor management, and organizational efficiency — turning operational complexity into repeatable, measurable performance."
        ),
        AgencyAgent(
            id = "agency-organizational-psychologist",
            name = "Organizational Psychologist",
            slug = "agency-organizational-psychologist",
            division = "Qualité & Tests",
            role = "Organizational Psychologist",
            description = "Applied organizational psychologist who diagnoses team dynamics, psychological safety, burnout risk, and culture health — using evidence-based frameworks to help leaders build high-performing, resilient, and psychologically safe organizations."
        ),
        AgencyAgent(
            id = "agency-orgscript-engineer",
            name = "Orgscript Engineer",
            slug = "agency-orgscript-engineer",
            division = "Ingénierie & Dev",
            role = "Orgscript Engineer",
            description = "Expert in designing, parsing, and implementing OrgScript grammar, AST validation, and business logic definitions."
        ),
        AgencyAgent(
            id = "agency-outbound-strategist",
            name = "Outbound Strategist",
            slug = "agency-outbound-strategist",
            division = "Design & UI/UX",
            role = "Outbound Strategist",
            description = "Signal-based outbound specialist who designs multi-channel prospecting sequences, defines ICPs, and builds pipeline through research-driven personalization — not volume."
        ),
        AgencyAgent(
            id = "agency-paid-media-auditor",
            name = "Paid Media Auditor",
            slug = "agency-paid-media-auditor",
            division = "Qualité & Tests",
            role = "Paid Media Auditor",
            description = "Comprehensive paid media auditor who systematically evaluates Google Ads, Microsoft Ads, and Meta accounts across 200+ checkpoints spanning account structure, tracking, bidding, creative, audiences, and competitive positioning. Produces actionable audit reports with prioritized recommendations and projected impact."
        ),
        AgencyAgent(
            id = "agency-paid-social-strategist",
            name = "Paid Social Strategist",
            slug = "agency-paid-social-strategist",
            division = "Design & UI/UX",
            role = "Paid Social Strategist",
            description = "Cross-platform paid social advertising specialist covering Meta (Facebook/Instagram), LinkedIn, TikTok, Pinterest, X, and Snapchat. Designs full-funnel social ad programs from prospecting through retargeting with platform-specific creative and audience strategies."
        ),
        AgencyAgent(
            id = "agency-payments-billing-engineer",
            name = "Payments Billing Engineer",
            slug = "agency-payments-billing-engineer",
            division = "Ingénierie & Dev",
            role = "Payments Billing Engineer",
            description = "Expert payments engineer for PSP integrations (Stripe, Adyen, Braintree, PayPal), idempotent payment flows, webhook processing, subscription billing, SCA/3DS, PCI scope reduction, and financial reconciliation."
        ),
        AgencyAgent(
            id = "agency-pdf-engine-architect",
            name = "Pdf Engine Architect",
            slug = "agency-pdf-engine-architect",
            division = "Architecture & Systèmes",
            role = "Pdf Engine Architect",
            description = "Architect and specialist in deterministic HTML-to-PDF document compilation, Playwright browser context pools, dynamic Euclidean page sizing, LayoutNG subpixel budgeting, tagged PDF (PDF/UA-1 & PDF/A-2b), and 1:1 sheet canvas editors."
        ),
        AgencyAgent(
            id = "agency-penetration-tester",
            name = "Penetration Tester",
            slug = "agency-penetration-tester",
            division = "Qualité & Tests",
            role = "Penetration Tester",
            description = "Offensive security specialist conducting authorized penetration tests, red team operations, and vulnerability assessments across networks, web applications, and cloud infrastructure."
        ),
        AgencyAgent(
            id = "agency-performance-benchmarker",
            name = "Performance Benchmarker",
            slug = "agency-performance-benchmarker",
            division = "Qualité & Tests",
            role = "Performance Benchmarker",
            description = "Expert performance testing and optimization specialist focused on measuring, analyzing, and improving system performance across all applications and infrastructure"
        ),
        AgencyAgent(
            id = "agency-persona-walkthrough-specialist",
            name = "Persona Walkthrough Specialist",
            slug = "agency-persona-walkthrough-specialist",
            division = "RH & Organisation",
            role = "Persona Walkthrough Specialist",
            description = "Simulate cognitive walkthroughs of web pages from a defined persona''s psychological perspective — captures emotional reactions and rational thought at each scroll position, then delivers structured CRO reports grounded in LIFT, Cialdini, and Fogg frameworks"
        ),
        AgencyAgent(
            id = "agency-personal-growth-mentor",
            name = "Personal Growth Mentor",
            slug = "agency-personal-growth-mentor",
            division = "Design & UI/UX",
            role = "Personal Growth Mentor",
            description = "Cross-domain personal development mentor for goal clarity, habit design, strategic decisions, and accountability without motivational fluff."
        ),
        AgencyAgent(
            id = "agency-pipeline-analyst",
            name = "Pipeline Analyst",
            slug = "agency-pipeline-analyst",
            division = "Data & AI",
            role = "Pipeline Analyst",
            description = "Revenue operations analyst specializing in pipeline health diagnostics, deal velocity analysis, forecast accuracy, and data-driven sales coaching. Turns CRM data into actionable pipeline intelligence that surfaces risks before they become missed quarters."
        ),
        AgencyAgent(
            id = "agency-platform-engineer",
            name = "Platform Engineer",
            slug = "agency-platform-engineer",
            division = "Ingénierie & Dev",
            role = "Platform Engineer",
            description = "Expert internal developer platform (IDP) engineer specializing in golden paths, paved roads, and self-serve infrastructure that multiplies engineering velocity."
        ),
        AgencyAgent(
            id = "agency-podcast-strategist",
            name = "Podcast Strategist",
            slug = "agency-podcast-strategist",
            division = "Design & UI/UX",
            role = "Podcast Strategist",
            description = "Content strategy and operations expert for the Chinese podcast market, with deep expertise in Xiaoyuzhou, Ximalaya, and other major audio platforms, covering show positioning, audio production, audience growth, multi-platform distribution, and monetization to help podcast creators build sticky audio content brands."
        ),
        AgencyAgent(
            id = "agency-ppc-campaign-strategist",
            name = "Ppc Campaign Strategist",
            slug = "agency-ppc-campaign-strategist",
            division = "Architecture & Systèmes",
            role = "Ppc Campaign Strategist",
            description = "Senior paid media strategist specializing in large-scale search, shopping, and performance max campaign architecture across Google, Microsoft, and Amazon ad platforms. Designs account structures, budget allocation frameworks, and bidding strategies that scale from \$10K to \$10M+ monthly spend."
        ),
        AgencyAgent(
            id = "agency-pr-communications-manager",
            name = "Pr Communications Manager",
            slug = "agency-pr-communications-manager",
            division = "Design & UI/UX",
            role = "Pr Communications Manager",
            description = "Strategic public relations and communications specialist for media relations, press releases, crisis communications, executive thought leadership, brand reputation management, and integrated communications planning — building and protecting reputations through earned media, storytelling, and proactive narrative control"
        ),
        AgencyAgent(
            id = "agency-pricing-analyst",
            name = "Pricing Analyst",
            slug = "agency-pricing-analyst",
            division = "Data & AI",
            role = "Pricing Analyst",
            description = "Specialized pricing analyst who develops optimal pricing models through market research, competitor analysis, cost structure evaluation, and margin optimization — turning pricing from guesswork into a data-driven competitive advantage."
        ),
        AgencyAgent(
            id = "agency-privacy-engineer",
            name = "Privacy Engineer",
            slug = "agency-privacy-engineer",
            division = "Ingénierie & Dev",
            role = "Privacy Engineer",
            description = "Expert privacy engineer who implements privacy in code — PII discovery and classification, data minimization, consent enforcement at the API layer, automated DSAR and deletion across services, pseudonymization/tokenization, and retention automation. Builds the technical controls a privacy policy only promises."
        ),
        AgencyAgent(
            id = "agency-private-domain-operator",
            name = "Private Domain Operator",
            slug = "agency-private-domain-operator",
            division = "Architecture & Systèmes",
            role = "Private Domain Operator",
            description = "Expert in building enterprise WeChat (WeCom) private domain ecosystems, with deep expertise in SCRM systems, segmented community operations, Mini Program commerce integration, user lifecycle management, and full-funnel conversion optimization."
        ),
        AgencyAgent(
            id = "agency-product-manager",
            name = "Product Manager",
            slug = "agency-product-manager",
            division = "Product & Project",
            role = "Product Manager",
            description = "Holistic product leader who owns the full product lifecycle — from discovery and strategy through roadmap, stakeholder alignment, go-to-market, and outcome measurement. Bridges business goals, user needs, and technical reality to ship the right thing at the right time."
        ),
        AgencyAgent(
            id = "agency-programmatic-display-buyer",
            name = "Programmatic Display Buyer",
            slug = "agency-programmatic-display-buyer",
            division = "DevOps & Cloud",
            role = "Programmatic Display Buyer",
            description = "Display advertising and programmatic media buying specialist covering managed placements, Google Display Network, DV360, trade desk platforms, partner media (newsletters, sponsored content), and ABM display strategies via platforms like Demandbase and 6Sense."
        ),
        AgencyAgent(
            id = "agency-project-shepherd",
            name = "Project Shepherd",
            slug = "agency-project-shepherd",
            division = "Product & Project",
            role = "Project Shepherd",
            description = "Expert project manager specializing in cross-functional project coordination, timeline management, and stakeholder alignment. Focused on shepherding projects from conception to completion while managing resources, risks, and communications across multiple teams and departments."
        ),
        AgencyAgent(
            id = "agency-prompt-engineer",
            name = "Prompt Engineer",
            slug = "agency-prompt-engineer",
            division = "Qualité & Tests",
            role = "Prompt Engineer",
            description = "Specialist in crafting, testing, and systematically optimizing prompts for LLMs — turning vague instructions into reliable, production-grade AI behaviors."
        ),
        AgencyAgent(
            id = "agency-proposal-strategist",
            name = "Proposal Strategist",
            slug = "agency-proposal-strategist",
            division = "Architecture & Systèmes",
            role = "Proposal Strategist",
            description = "Strategic proposal architect who transforms RFPs and sales opportunities into compelling win narratives. Specializes in win theme development, competitive positioning, executive summary craft, and building proposals that persuade rather than merely comply."
        ),
        AgencyAgent(
            id = "agency-psychologist",
            name = "Psychologist",
            slug = "agency-psychologist",
            division = "Design & UI/UX",
            role = "Psychologist",
            description = "Expert in human behavior, personality theory, motivation, and cognitive patterns — builds psychologically credible characters and interactions grounded in clinical and research frameworks"
        ),
        AgencyAgent(
            id = "agency-rag-pipeline-engineer",
            name = "Rag Pipeline Engineer",
            slug = "agency-rag-pipeline-engineer",
            division = "Qualité & Tests",
            role = "Rag Pipeline Engineer",
            description = "Production RAG specialist focused on chunking strategy, retrieval quality, hybrid search, re-ranking, and eval-driven iteration. Builds pipelines that actually retrieve the right context — not just pipelines that run."
        ),
        AgencyAgent(
            id = "agency-rapid-prototyper",
            name = "Rapid Prototyper",
            slug = "agency-rapid-prototyper",
            division = "Ingénierie & Dev",
            role = "Rapid Prototyper",
            description = "Specialized in ultra-fast proof-of-concept development and MVP creation using efficient tools and frameworks"
        ),
        AgencyAgent(
            id = "agency-real-estate-buyer-seller",
            name = "Real Estate Buyer Seller",
            slug = "agency-real-estate-buyer-seller",
            division = "Support & Client",
            role = "Real Estate Buyer Seller",
            description = "Comprehensive real estate agent assistant for buyer representation, seller representation, listing management, offer negotiation, transaction coordination, and closing support — delivering a world-class client experience from first showing to final closing across residential and investment real estate"
        ),
        AgencyAgent(
            id = "agency-reality-checker",
            name = "Reality Checker",
            slug = "agency-reality-checker",
            division = "Qualité & Tests",
            role = "Reality Checker",
            description = "Stops fantasy approvals, evidence-based certification - Default to \"NEEDS WORK\", requires overwhelming proof for production readiness"
        ),
        AgencyAgent(
            id = "agency-realtime-collaboration-engineer",
            name = "Realtime Collaboration Engineer",
            slug = "agency-realtime-collaboration-engineer",
            division = "Ingénierie & Dev",
            role = "Realtime Collaboration Engineer",
            description = "Expert realtime systems engineer for WebSocket/SSE infrastructure, presence, CRDT and OT-based collaborative editing, offline-first sync engines, and fan-out scaling with reconnect-safe protocols."
        ),
        AgencyAgent(
            id = "agency-recruitment-specialist",
            name = "Recruitment Specialist",
            slug = "agency-recruitment-specialist",
            division = "Design & UI/UX",
            role = "Recruitment Specialist",
            description = "Expert recruitment operations and talent acquisition specialist — skilled in China''s major hiring platforms, talent assessment frameworks, and labor law compliance. Helps companies efficiently attract, screen, and retain top talent while building a competitive employer brand."
        ),
        AgencyAgent(
            id = "agency-reddit-community-builder",
            name = "Reddit Community Builder",
            slug = "agency-reddit-community-builder",
            division = "Design & UI/UX",
            role = "Reddit Community Builder",
            description = "Expert Reddit marketing specialist focused on authentic community engagement, value-driven content creation, and long-term relationship building. Masters Reddit culture navigation."
        ),
        AgencyAgent(
            id = "agency-report-distribution-agent",
            name = "Report Distribution Agent",
            slug = "agency-report-distribution-agent",
            division = "Data & AI",
            role = "Report Distribution Agent",
            description = "AI agent that automates distribution of consolidated sales reports to representatives based on territorial parameters"
        ),
        AgencyAgent(
            id = "agency-research-synthesist",
            name = "Research Synthesist",
            slug = "agency-research-synthesist",
            division = "Qualité & Tests",
            role = "Research Synthesist",
            description = "Expert in literature review, source evaluation, and evidence synthesis — turns a scattered pile of sources into a structured, honestly-weighted map of what the evidence actually supports"
        ),
        AgencyAgent(
            id = "agency-resume-tailor",
            name = "Resume Tailor",
            slug = "agency-resume-tailor",
            division = "Design & UI/UX",
            role = "Resume Tailor",
            description = "Candidate-side resume optimization specialist who analyzes job descriptions, maps real experience to role requirements, improves ATS keyword alignment, and rewrites bullets without fabricating qualifications."
        ),
        AgencyAgent(
            id = "agency-retail-customer-returns",
            name = "Retail Customer Returns",
            slug = "agency-retail-customer-returns",
            division = "Data & AI",
            role = "Retail Customer Returns",
            description = "Comprehensive retail customer returns specialist for processing returns, exchanges, and refunds across in-store, online, and omnichannel retail — handling policy enforcement, fraud prevention, customer retention, vendor returns, and returns analytics to maximize recovery while preserving customer loyalty"
        ),
        AgencyAgent(
            id = "agency-roblox-avatar-creator",
            name = "Roblox Avatar Creator",
            slug = "agency-roblox-avatar-creator",
            division = "Architecture & Systèmes",
            role = "Roblox Avatar Creator",
            description = "Roblox UGC and avatar pipeline specialist - Masters Roblox''s avatar system, UGC item creation, accessory rigging, texture standards, and the Creator Marketplace submission pipeline"
        ),
        AgencyAgent(
            id = "agency-roblox-experience-designer",
            name = "Roblox Experience Designer",
            slug = "agency-roblox-experience-designer",
            division = "Ingénierie & Dev",
            role = "Roblox Experience Designer",
            description = "Roblox platform UX and monetization specialist - Masters engagement loop design, DataStore-driven progression, Roblox monetization systems (Passes, Developer Products, UGC), and player retention for Roblox experiences"
        ),
        AgencyAgent(
            id = "agency-roblox-systems-scripter",
            name = "Roblox Systems Scripter",
            slug = "agency-roblox-systems-scripter",
            division = "Cybersécurité",
            role = "Roblox Systems Scripter",
            description = "Roblox platform engineering specialist - Masters Luau, the client-server security model, RemoteEvents/RemoteFunctions, DataStore, and module architecture for scalable Roblox experiences"
        ),
        AgencyAgent(
            id = "agency-rust-refactoring-specialist",
            name = "Rust Refactoring Specialist",
            slug = "agency-rust-refactoring-specialist",
            division = "Ingénierie & Dev",
            role = "Rust Refactoring Specialist",
            description = "Expert Rust engineer for repository-scale refactoring, safe renames, module restructuring, duplication removal, panic hardening, ownership improvements, and compiler or Clippy remediation."
        ),
        AgencyAgent(
            id = "agency-sales-coach",
            name = "Sales Coach",
            slug = "agency-sales-coach",
            division = "Stratégie & Ventes",
            role = "Sales Coach",
            description = "Expert sales coaching specialist focused on rep development, pipeline review facilitation, call coaching, deal strategy, and forecast accuracy. Makes every rep and every deal better through structured coaching methodology and behavioral feedback."
        ),
        AgencyAgent(
            id = "agency-sales-data-extraction-agent",
            name = "Sales Data Extraction Agent",
            slug = "agency-sales-data-extraction-agent",
            division = "Data & AI",
            role = "Sales Data Extraction Agent",
            description = "AI agent specialized in monitoring Excel files and extracting key sales metrics (MTD, YTD, Year End) for internal live reporting"
        ),
        AgencyAgent(
            id = "agency-sales-engineer",
            name = "Sales Engineer",
            slug = "agency-sales-engineer",
            division = "Ingénierie & Dev",
            role = "Sales Engineer",
            description = "Senior pre-sales engineer specializing in technical discovery, demo engineering, POC scoping, competitive battlecards, and bridging product capabilities to business outcomes. Wins the technical decision so the deal can close."
        ),
        AgencyAgent(
            id = "agency-sales-outreach",
            name = "Sales Outreach",
            slug = "agency-sales-outreach",
            division = "Design & UI/UX",
            role = "Sales Outreach",
            description = "Consultative B2B sales outreach specialist for cold prospecting, lead follow-up, objection handling, proposal writing, and pipeline management — combining data-driven targeting with genuine relationship-building to open doors and close deals"
        ),
        AgencyAgent(
            id = "agency-salesforce-architect",
            name = "Salesforce Architect",
            slug = "agency-salesforce-architect",
            division = "Architecture & Systèmes",
            role = "Salesforce Architect",
            description = "Solution architecture for Salesforce platform — multi-cloud design, integration patterns, governor limits, deployment strategy, and data model governance for enterprise-scale orgs"
        ),
        AgencyAgent(
            id = "agency-search-query-analyst",
            name = "Search Query Analyst",
            slug = "agency-search-query-analyst",
            division = "Architecture & Systèmes",
            role = "Search Query Analyst",
            description = "Specialist in search term analysis, negative keyword architecture, and query-to-intent mapping. Turns raw search query data into actionable optimizations that eliminate waste and amplify high-intent traffic across paid search accounts."
        ),
        AgencyAgent(
            id = "agency-search-relevance-engineer",
            name = "Search Relevance Engineer",
            slug = "agency-search-relevance-engineer",
            division = "Ingénierie & Dev",
            role = "Search Relevance Engineer",
            description = "Expert search engineer for Elasticsearch and OpenSearch — index and analyzer design, BM25 query tuning, hybrid lexical+vector retrieval, and judgment-based relevance evaluation with nDCG and online experiments."
        ),
        AgencyAgent(
            id = "agency-secrets-credential-hygiene-engineer",
            name = "Secrets Credential Hygiene Engineer",
            slug = "agency-secrets-credential-hygiene-engineer",
            division = "Ingénierie & Dev",
            role = "Secrets Credential Hygiene Engineer",
            description = "Owns the full lifecycle of secrets and credentials — detection, prevention, vaulting, rotation, and leak response — so an application runs on short-lived, least-privilege credentials that are never in the code and are already rotated by the time a leak is found."
        ),
        AgencyAgent(
            id = "agency-section-508-accessibility-specialist",
            name = "Section 508 Accessibility Specialist",
            slug = "agency-section-508-accessibility-specialist",
            division = "Qualité & Tests",
            role = "Section 508 Accessibility Specialist",
            description = "Expert U.S. federal Section 508 accessibility engineer (the 508 legal baseline is WCAG 2.0 Level AA; WCAG 2.1/2.2 AA are recommended best practice, and ADA Title II requires WCAG 2.1 AA for state/local government) specializing in accessible web development, ARIA implementation, screen reader testing (JAWS/NVDA/VoiceOver), keyboard navigation, color contrast, accessible forms and PDFs, VPAT/ACR authoring, automated and manual auditing (axe/WAVE/Lighthouse), and remediation for government and enterprise sites"
        ),
        AgencyAgent(
            id = "agency-security-architect",
            name = "Security Architect",
            slug = "agency-security-architect",
            division = "Cybersécurité",
            role = "Security Architect",
            description = "Expert security architect specializing in threat modeling, secure-by-design architecture, trust-boundary analysis, defense-in-depth, and risk-based security reviews across web, API, cloud-native, and distributed systems. Designs the security model; hands code-level SAST/DAST and SDLC work to the AppSec Engineer."
        ),
        AgencyAgent(
            id = "agency-senior-developer",
            name = "Senior Developer",
            slug = "agency-senior-developer",
            division = "Ingénierie & Dev",
            role = "Senior Developer",
            description = "Premium implementation specialist - Masters Laravel/Livewire/FluxUI, advanced CSS, Three.js integration"
        ),
        AgencyAgent(
            id = "agency-senior-project-manager",
            name = "Senior Project Manager",
            slug = "agency-senior-project-manager",
            division = "Design & UI/UX",
            role = "Senior Project Manager",
            description = "Converts specs to tasks and remembers previous projects. Focused on realistic scope, no background processes, exact spec requirements"
        ),
        AgencyAgent(
            id = "agency-senior-secops-engineer",
            name = "Senior Secops Engineer",
            slug = "agency-senior-secops-engineer",
            division = "Qualité & Tests",
            role = "Senior Secops Engineer",
            description = "Defensive application security specialist who scans every code submission for secrets and sensitive data exposure before anything else, then implements or audits security controls following the organization''s security standard — covering authentication, authorization, tokens, cookies, HTTP headers, CORS, rate limiting, CSP, secrets management, input validation, and secure logging."
        ),
        AgencyAgent(
            id = "agency-seo-specialist",
            name = "Seo Specialist",
            slug = "agency-seo-specialist",
            division = "Design & UI/UX",
            role = "Seo Specialist",
            description = "Expert search engine optimization strategist specializing in technical SEO, content optimization, link authority building, and organic search growth. Drives sustainable traffic through data-driven search strategies."
        ),
        AgencyAgent(
            id = "agency-servicenow-developer-mentor",
            name = "Servicenow Developer Mentor",
            slug = "agency-servicenow-developer-mentor",
            division = "Ingénierie & Dev",
            role = "Servicenow Developer Mentor",
            description = "ServiceNow platform developer and step-by-step troubleshooter — Business Rules, Script Includes, GlideRecord/GlideAggregate, Flow Designer, ACLs, and \"is this OOTB or did custom break it?\" isolation"
        ),
        AgencyAgent(
            id = "agency-short-video-editing-coach",
            name = "Short Video Editing Coach",
            slug = "agency-short-video-editing-coach",
            division = "Ingénierie & Dev",
            role = "Short Video Editing Coach",
            description = "Hands-on short-video editing coach covering the full post-production pipeline, with mastery of CapCut Pro, Premiere Pro, DaVinci Resolve, and Final Cut Pro across composition and camera language, color grading, audio engineering, motion graphics and VFX, subtitle design, multi-platform export optimization, editing workflow efficiency, and AI-assisted editing."
        ),
        AgencyAgent(
            id = "agency-social-media-strategist",
            name = "Social Media Strategist",
            slug = "agency-social-media-strategist",
            division = "Design & UI/UX",
            role = "Social Media Strategist",
            description = "Expert social media strategist for LinkedIn, Twitter, and professional platforms. Creates cross-platform campaigns, builds communities, manages real-time engagement, and develops thought leadership strategies."
        ),
        AgencyAgent(
            id = "agency-software-architect",
            name = "Software Architect",
            slug = "agency-software-architect",
            division = "Architecture & Systèmes",
            role = "Software Architect",
            description = "Expert software architect specializing in system design, domain-driven design, architectural patterns, and technical decision-making for scalable, maintainable systems."
        ),
        AgencyAgent(
            id = "agency-solidity-smart-contract-engineer",
            name = "Solidity Smart Contract Engineer",
            slug = "agency-solidity-smart-contract-engineer",
            division = "Cybersécurité",
            role = "Solidity Smart Contract Engineer",
            description = "Expert Solidity developer specializing in EVM smart contract architecture, gas optimization, upgradeable proxy patterns, DeFi protocol development, and security-first contract design across Ethereum and L2 chains."
        ),
        AgencyAgent(
            id = "agency-solution-engineer",
            name = "Solution Engineer",
            slug = "agency-solution-engineer",
            division = "Ingénierie & Dev",
            role = "Solution Engineer",
            description = "Hands-on GIS prototype builder who takes strategy from Technical Consultant and turns it into working demos, proof-of-concepts, and technical validations across the full Esri and open-source stack."
        ),
        AgencyAgent(
            id = "agency-sovereign-health-systems-agent",
            name = "Sovereign Health Systems Agent",
            slug = "agency-sovereign-health-systems-agent",
            division = "Architecture & Systèmes",
            role = "Sovereign Health Systems Agent",
            description = "Government health mandate engagement framework for AI agents operating at the intersection of national health infrastructure, UHC policy, and emerging market deployment. Defines how to navigate sovereign health ministry engagement, frame health technology for mandate alignment, and sequence a dual-market launch across regulated and sovereign contexts."
        ),
        AgencyAgent(
            id = "agency-spatial-data-engineer",
            name = "Spatial Data Engineer",
            slug = "agency-spatial-data-engineer",
            division = "Ingénierie & Dev",
            role = "Spatial Data Engineer",
            description = "ETL specialist who transforms messy geospatial data from any source into clean, standardized, production-ready datasets — format conversion, CRS reprojection, attribute normalization, and automated pipelines."
        ),
        AgencyAgent(
            id = "agency-spatial-data-scientist",
            name = "Spatial Data Scientist",
            slug = "agency-spatial-data-scientist",
            division = "Data & AI",
            role = "Spatial Data Scientist",
            description = "Advanced spatial analytics specialist who applies statistical modeling, spatial econometrics, clustering, and predictive analytics to geospatial data — finding patterns that aren''t visible on a map."
        ),
        AgencyAgent(
            id = "agency-sprint-prioritizer",
            name = "Sprint Prioritizer",
            slug = "agency-sprint-prioritizer",
            division = "Data & AI",
            role = "Sprint Prioritizer",
            description = "Expert product manager specializing in agile sprint planning, feature prioritization, and resource allocation. Focused on maximizing team velocity and business value delivery through data-driven prioritization frameworks."
        ),
        AgencyAgent(
            id = "agency-sre-site-reliability-engineer",
            name = "Sre Site Reliability Engineer",
            slug = "agency-sre-site-reliability-engineer",
            division = "Ingénierie & Dev",
            role = "Sre Site Reliability Engineer",
            description = "Expert site reliability engineer specializing in SLOs, error budgets, observability, chaos engineering, and toil reduction for production systems at scale."
        ),
        AgencyAgent(
            id = "agency-statistician",
            name = "Statistician",
            slug = "agency-statistician",
            division = "Qualité & Tests",
            role = "Statistician",
            description = "Expert in quantitative research methodology, experimental design, and statistical inference — pressure-tests claims, designs sound studies, and separates real signal from noise, chance, and bias"
        ),
        AgencyAgent(
            id = "agency-strategy-duel-agent",
            name = "Strategy Duel Agent",
            slug = "agency-strategy-duel-agent",
            division = "Game Development",
            role = "Strategy Duel Agent",
            description = "Conducts live strategy duels using game theory and the 36 Chinese stratagems"
        ),
        AgencyAgent(
            id = "agency-studio-operations",
            name = "Studio Operations",
            slug = "agency-studio-operations",
            division = "Data & AI",
            role = "Studio Operations",
            description = "Expert operations manager specializing in day-to-day studio efficiency, process optimization, and resource coordination. Focused on ensuring smooth operations, maintaining productivity standards, and supporting all teams with the tools and processes needed for success."
        ),
        AgencyAgent(
            id = "agency-studio-producer",
            name = "Studio Producer",
            slug = "agency-studio-producer",
            division = "Design & UI/UX",
            role = "Studio Producer",
            description = "Senior strategic leader specializing in high-level creative and technical project orchestration, resource allocation, and multi-project portfolio management. Focused on aligning creative vision with business objectives while managing complex cross-functional initiatives and ensuring optimal studio operations."
        ),
        AgencyAgent(
            id = "agency-study-abroad-advisor",
            name = "Study Abroad Advisor",
            slug = "agency-study-abroad-advisor",
            division = "Qualité & Tests",
            role = "Study Abroad Advisor",
            description = "Full-spectrum study abroad planning expert covering the US, UK, Canada, Australia, Europe, Hong Kong, and Singapore — proficient in undergraduate, master''s, and PhD application strategy, school selection, essay coaching, profile enhancement, standardized test planning, visa preparation, and overseas life adaptation, helping Chinese students craft personalized end-to-end study abroad plans."
        ),
        AgencyAgent(
            id = "agency-supply-chain-strategist",
            name = "Supply Chain Strategist",
            slug = "agency-supply-chain-strategist",
            division = "Qualité & Tests",
            role = "Supply Chain Strategist",
            description = "Expert supply chain management and procurement strategy specialist — skilled in supplier development, strategic sourcing, quality control, and supply chain digitalization. Grounded in China''s manufacturing ecosystem, helps companies build efficient, resilient, and sustainable supply chains."
        ),
        AgencyAgent(
            id = "agency-support-responder",
            name = "Support Responder",
            slug = "agency-support-responder",
            division = "Support & Client",
            role = "Support Responder",
            description = "Expert customer support specialist delivering exceptional customer service, issue resolution, and user experience optimization. Specializes in multi-channel support, proactive customer care, and turning support interactions into positive brand experiences."
        ),
        AgencyAgent(
            id = "agency-tax-strategist",
            name = "Tax Strategist",
            slug = "agency-tax-strategist",
            division = "GIS & Spatial",
            role = "Tax Strategist",
            description = "Expert tax strategist specializing in tax optimization, multi-jurisdictional compliance, transfer pricing, and strategic tax planning. Navigates complex tax codes to minimize liability while ensuring full regulatory compliance across local, state, federal, and international tax regimes."
        ),
        AgencyAgent(
            id = "agency-technical-artist",
            name = "Technical Artist",
            slug = "agency-technical-artist",
            division = "Architecture & Systèmes",
            role = "Technical Artist",
            description = "Art-to-engine pipeline specialist - Masters shaders, VFX systems, LOD pipelines, performance budgeting, and cross-engine asset optimization"
        ),
        AgencyAgent(
            id = "agency-technical-consultant",
            name = "Technical Consultant",
            slug = "agency-technical-consultant",
            division = "Architecture & Systèmes",
            role = "Technical Consultant",
            description = "Strategic GIS advisor who translates business problems into geospatial solutions — gap analysis, technology roadmaps, RFP responses, and digital transformation strategy across Esri and open-source ecosystems."
        ),
        AgencyAgent(
            id = "agency-technical-writer",
            name = "Technical Writer",
            slug = "agency-technical-writer",
            division = "Ingénierie & Dev",
            role = "Technical Writer",
            description = "Expert technical writer specializing in developer documentation, API references, README files, and tutorials. Transforms complex engineering concepts into clear, accurate, and engaging docs that developers actually read and use."
        ),
        AgencyAgent(
            id = "agency-terminal-integration-specialist",
            name = "Terminal Integration Specialist",
            slug = "agency-terminal-integration-specialist",
            division = "Ingénierie & Dev",
            role = "Terminal Integration Specialist",
            description = "Terminal emulation, text rendering optimization, and SwiftTerm integration for modern Swift applications"
        ),
        AgencyAgent(
            id = "agency-test-automation-engineer",
            name = "Test Automation Engineer",
            slug = "agency-test-automation-engineer",
            division = "Qualité & Tests",
            role = "Test Automation Engineer",
            description = "Expert end-to-end test automation engineer for Playwright and Cypress — resilient selectors, flake elimination, isolated test data, CI parallelization, and trace-driven failure debugging."
        ),
        AgencyAgent(
            id = "agency-test-results-analyzer",
            name = "Test Results Analyzer",
            slug = "agency-test-results-analyzer",
            division = "Qualité & Tests",
            role = "Test Results Analyzer",
            description = "Expert test analysis specialist focused on comprehensive test result evaluation, quality metrics analysis, and actionable insight generation from testing activities"
        ),
        AgencyAgent(
            id = "agency-threat-detection-engineer",
            name = "Threat Detection Engineer",
            slug = "agency-threat-detection-engineer",
            division = "Cybersécurité",
            role = "Threat Detection Engineer",
            description = "Expert detection engineer specializing in SIEM rule development, MITRE ATT&CK coverage mapping, threat hunting, alert tuning, and detection-as-code pipelines for security operations teams."
        ),
        AgencyAgent(
            id = "agency-threat-intelligence-analyst",
            name = "Threat Intelligence Analyst",
            slug = "agency-threat-intelligence-analyst",
            division = "Cybersécurité",
            role = "Threat Intelligence Analyst",
            description = "Cyber threat intelligence specialist who tracks adversary groups, maps attack campaigns to MITRE ATT&CK, produces actionable intelligence reports, and builds detection rules that catch real threats."
        ),
        AgencyAgent(
            id = "agency-tiktok-strategist",
            name = "Tiktok Strategist",
            slug = "agency-tiktok-strategist",
            division = "Design & UI/UX",
            role = "Tiktok Strategist",
            description = "Expert TikTok marketing specialist focused on viral content creation, algorithm optimization, and community building. Masters TikTok''s unique culture and features for brand growth."
        ),
        AgencyAgent(
            id = "agency-tool-evaluator",
            name = "Tool Evaluator",
            slug = "agency-tool-evaluator",
            division = "Qualité & Tests",
            role = "Tool Evaluator",
            description = "Expert technology assessment specialist focused on evaluating, testing, and recommending tools, software, and platforms for business use and productivity optimization"
        ),
        AgencyAgent(
            id = "agency-tracking-measurement-specialist",
            name = "Tracking Measurement Specialist",
            slug = "agency-tracking-measurement-specialist",
            division = "Ingénierie & Dev",
            role = "Tracking Measurement Specialist",
            description = "Expert in conversion tracking architecture, tag management, and attribution modeling across Google Tag Manager, GA4, Google Ads, Meta CAPI, LinkedIn Insight Tag, and server-side implementations. Ensures every conversion is counted correctly and every dollar of ad spend is measurable."
        ),
        AgencyAgent(
            id = "agency-trend-researcher",
            name = "Trend Researcher",
            slug = "agency-trend-researcher",
            division = "Product & Project",
            role = "Trend Researcher",
            description = "Expert market intelligence analyst specializing in identifying emerging trends, competitive analysis, and opportunity assessment. Focused on providing actionable insights that drive product strategy and innovation decisions."
        ),
        AgencyAgent(
            id = "agency-twitter-engager",
            name = "Twitter Engager",
            slug = "agency-twitter-engager",
            division = "Design & UI/UX",
            role = "Twitter Engager",
            description = "Expert Twitter marketing specialist focused on real-time engagement, thought leadership building, and community-driven growth. Builds brand authority through authentic conversation participation and viral thread creation."
        ),
        AgencyAgent(
            id = "agency-ui-designer",
            name = "Ui Designer",
            slug = "agency-ui-designer",
            division = "Architecture & Systèmes",
            role = "Ui Designer",
            description = "Expert UI designer specializing in visual design systems, component libraries, and pixel-perfect interface creation. Creates beautiful, consistent, accessible user interfaces that enhance UX and reflect brand identity"
        ),
        AgencyAgent(
            id = "agency-ui-finish-gate-reviewer",
            name = "Ui Finish Gate Reviewer",
            slug = "agency-ui-finish-gate-reviewer",
            division = "Qualité & Tests",
            role = "Ui Finish Gate Reviewer",
            description = "Product-interface reviewer who catches generic, interchangeable UI before it ships by grounding critique in real product evidence, a written design contract, and a hard implementation finish gate."
        ),
        AgencyAgent(
            id = "agency-unity-architect",
            name = "Unity Architect",
            slug = "agency-unity-architect",
            division = "Architecture & Systèmes",
            role = "Unity Architect",
            description = "Data-driven modularity specialist - Masters ScriptableObjects, decoupled systems, and single-responsibility component design for scalable Unity projects"
        ),
        AgencyAgent(
            id = "agency-unity-editor-tool-developer",
            name = "Unity Editor Tool Developer",
            slug = "agency-unity-editor-tool-developer",
            division = "Ingénierie & Dev",
            role = "Unity Editor Tool Developer",
            description = "Unity editor automation specialist - Masters custom EditorWindows, PropertyDrawers, AssetPostprocessors, ScriptedImporters, and pipeline automation that saves teams hours per week"
        ),
        AgencyAgent(
            id = "agency-unity-multiplayer-engineer",
            name = "Unity Multiplayer Engineer",
            slug = "agency-unity-multiplayer-engineer",
            division = "Ingénierie & Dev",
            role = "Unity Multiplayer Engineer",
            description = "Networked gameplay specialist - Masters Netcode for GameObjects, Unity Gaming Services (Relay/Lobby), client-server authority, lag compensation, and state synchronization"
        ),
        AgencyAgent(
            id = "agency-unity-shader-graph-artist",
            name = "Unity Shader Graph Artist",
            slug = "agency-unity-shader-graph-artist",
            division = "Design & UI/UX",
            role = "Unity Shader Graph Artist",
            description = "Visual effects and material specialist - Masters Unity Shader Graph, HLSL, URP/HDRP rendering pipelines, and custom pass authoring for real-time visual effects"
        ),
        AgencyAgent(
            id = "agency-universal-document-compiler",
            name = "Universal Document Compiler",
            slug = "agency-universal-document-compiler",
            division = "Architecture & Systèmes",
            role = "Universal Document Compiler",
            description = "Architect of schema-agnostic document ASTs, algorithmic data-shape layout inference, bidirectional CST-to-canvas synchronization, and universal paged document publishing."
        ),
        AgencyAgent(
            id = "agency-unreal-multiplayer-architect",
            name = "Unreal Multiplayer Architect",
            slug = "agency-unreal-multiplayer-architect",
            division = "Architecture & Systèmes",
            role = "Unreal Multiplayer Architect",
            description = "Unreal Engine networking specialist - Masters Actor replication, GameMode/GameState architecture, server-authoritative gameplay, network prediction, and dedicated server setup for UE5"
        ),
        AgencyAgent(
            id = "agency-unreal-systems-engineer",
            name = "Unreal Systems Engineer",
            slug = "agency-unreal-systems-engineer",
            division = "Ingénierie & Dev",
            role = "Unreal Systems Engineer",
            description = "Performance and hybrid architecture specialist - Masters C++/Blueprint continuum, Nanite geometry, Lumen GI, and Gameplay Ability System for AAA-grade Unreal Engine projects"
        ),
        AgencyAgent(
            id = "agency-unreal-technical-artist",
            name = "Unreal Technical Artist",
            slug = "agency-unreal-technical-artist",
            division = "Design & UI/UX",
            role = "Unreal Technical Artist",
            description = "Unreal Engine visual pipeline specialist - Masters the Material Editor, Niagara VFX, Procedural Content Generation, and the art-to-engine pipeline for UE5 projects"
        ),
        AgencyAgent(
            id = "agency-unreal-world-builder",
            name = "Unreal World Builder",
            slug = "agency-unreal-world-builder",
            division = "Design & UI/UX",
            role = "Unreal World Builder",
            description = "Open-world and environment specialist - Masters UE5 World Partition, Landscape, procedural foliage, HLOD, and large-scale level streaming for seamless open-world experiences"
        ),
        AgencyAgent(
            id = "agency-uswds-developer",
            name = "Uswds Developer",
            slug = "agency-uswds-developer",
            division = "Ingénierie & Dev",
            role = "Uswds Developer",
            description = "Expert U.S. Web Design System frontend developer specializing in USWDS components and design tokens, accessible-by-default patterns, responsive government UI, Sass settings/theming, the federal design language, integration into CMS platforms (Drupal/WordPress), and compliance with 21st Century IDEA and the Federal Website Standards"
        ),
        AgencyAgent(
            id = "agency-ux-architect",
            name = "Ux Architect",
            slug = "agency-ux-architect",
            division = "Ingénierie & Dev",
            role = "Ux Architect",
            description = "Technical architecture and UX specialist who provides developers with solid foundations, CSS systems, and clear implementation guidance"
        ),
        AgencyAgent(
            id = "agency-ux-researcher",
            name = "Ux Researcher",
            slug = "agency-ux-researcher",
            division = "Qualité & Tests",
            role = "Ux Researcher",
            description = "Expert user experience researcher specializing in user behavior analysis, usability testing, and data-driven design insights. Provides actionable research findings that improve product usability and user satisfaction"
        ),
        AgencyAgent(
            id = "agency-video-optimization-specialist",
            name = "Video Optimization Specialist",
            slug = "agency-video-optimization-specialist",
            division = "Data & AI",
            role = "Video Optimization Specialist",
            description = "Video marketing strategist specializing in YouTube algorithm optimization, audience retention, chaptering, thumbnail concepts, and cross-platform video syndication."
        ),
        AgencyAgent(
            id = "agency-video-streaming-engineer",
            name = "Video Streaming Engineer",
            slug = "agency-video-streaming-engineer",
            division = "Ingénierie & Dev",
            role = "Video Streaming Engineer",
            description = "Expert video streaming engineer for adaptive bitrate delivery — HLS/DASH packaging, ffmpeg transcode ladders, CMAF low-latency, DRM, CDN delivery, and QoE-driven player tuning."
        ),
        AgencyAgent(
            id = "agency-visionos-spatial-engineer",
            name = "Visionos Spatial Engineer",
            slug = "agency-visionos-spatial-engineer",
            division = "Ingénierie & Dev",
            role = "Visionos Spatial Engineer",
            description = "Native visionOS spatial computing, SwiftUI volumetric interfaces, and Liquid Glass design implementation"
        ),
        AgencyAgent(
            id = "agency-visual-storyteller",
            name = "Visual Storyteller",
            slug = "agency-visual-storyteller",
            division = "Design & UI/UX",
            role = "Visual Storyteller",
            description = "Expert visual communication specialist focused on creating compelling visual narratives, multimedia content, and brand storytelling through design. Specializes in transforming complex information into engaging visual stories that connect with audiences and drive emotional engagement."
        ),
        AgencyAgent(
            id = "agency-voice-ai-integration-engineer",
            name = "Voice Ai Integration Engineer",
            slug = "agency-voice-ai-integration-engineer",
            division = "Ingénierie & Dev",
            role = "Voice Ai Integration Engineer",
            description = "Expert in building end-to-end speech transcription pipelines using Whisper-style models and cloud ASR services — from raw audio ingestion through preprocessing, transcript cleanup, subtitle generation, speaker diarization, and structured downstream integration into apps, APIs, and CMS platforms."
        ),
        AgencyAgent(
            id = "agency-web-gis-developer",
            name = "Web Gis Developer",
            slug = "agency-web-gis-developer",
            division = "Ingénierie & Dev",
            role = "Web Gis Developer",
            description = "Full-stack web GIS engineer who builds interactive mapping applications — MapLibre GL JS, ArcGIS JS API, Leaflet, real-time dashboards, REST API integration, and geospatial web services."
        ),
        AgencyAgent(
            id = "agency-webassembly-engineer",
            name = "Webassembly Engineer",
            slug = "agency-webassembly-engineer",
            division = "Ingénierie & Dev",
            role = "Webassembly Engineer",
            description = "Expert WebAssembly engineer — compiling Rust/C++/Go to Wasm, JS interop and the boundary marshalling cost, WASI and server-side runtimes (Wasmtime/Wasmer), the component model, and near-native performance tuning."
        ),
        AgencyAgent(
            id = "agency-wechat-mini-program-developer",
            name = "Wechat Mini Program Developer",
            slug = "agency-wechat-mini-program-developer",
            division = "Ingénierie & Dev",
            role = "Wechat Mini Program Developer",
            description = "Expert WeChat Mini Program developer specializing in 小程序 development with WXML/WXSS/WXS, WeChat API integration, payment systems, subscription messaging, and the full WeChat ecosystem."
        ),
        AgencyAgent(
            id = "agency-wechat-official-account-manager",
            name = "Wechat Official Account Manager",
            slug = "agency-wechat-official-account-manager",
            division = "Design & UI/UX",
            role = "Wechat Official Account Manager",
            description = "Expert WeChat Official Account (OA) strategist specializing in content marketing, subscriber engagement, and conversion optimization. Masters multi-format content and builds loyal communities through consistent value delivery."
        ),
        AgencyAgent(
            id = "agency-weibo-strategist",
            name = "Weibo Strategist",
            slug = "agency-weibo-strategist",
            division = "Data & AI",
            role = "Weibo Strategist",
            description = "Full-spectrum operations expert for Sina Weibo, with deep expertise in trending topic mechanics, Super Topic community management, public sentiment monitoring, fan economy strategies, and Weibo advertising, helping brands achieve viral reach and sustained growth on China''s leading public discourse platform."
        ),
        AgencyAgent(
            id = "agency-whimsy-injector",
            name = "Whimsy Injector",
            slug = "agency-whimsy-injector",
            division = "Design & UI/UX",
            role = "Whimsy Injector",
            description = "Expert creative specialist focused on adding personality, delight, and playful elements to brand experiences. Creates memorable, joyful interactions that differentiate brands through unexpected moments of whimsy"
        ),
        AgencyAgent(
            id = "agency-wordpress-performance-engineer",
            name = "Wordpress Performance Engineer",
            slug = "agency-wordpress-performance-engineer",
            division = "Qualité & Tests",
            role = "Wordpress Performance Engineer",
            description = "Expert WordPress performance engineer specializing in Core Web Vitals, object caching (Redis/Memcached), page caching, database and WP_Query optimization, the Transients API, asset minification/deferral/critical CSS, image optimization and lazy loading, CDN integration, plugin performance auditing, and PHP-FPM/opcache tuning for fast, audit-passing sites"
        ),
        AgencyAgent(
            id = "agency-wordpress-shopping-cart-engineer",
            name = "Wordpress Shopping Cart Engineer",
            slug = "agency-wordpress-shopping-cart-engineer",
            division = "Ingénierie & Dev",
            role = "Wordpress Shopping Cart Engineer",
            description = "Expert WordPress e-commerce engineer specializing in WooCommerce for product catalog management, payment gateway integration, checkout customization, order management, tax and coupon configuration, and conversion-optimized storefront delivery on WordPress"
        ),
        AgencyAgent(
            id = "agency-workflow-architect",
            name = "Workflow Architect",
            slug = "agency-workflow-architect",
            division = "Qualité & Tests",
            role = "Workflow Architect",
            description = "Workflow design specialist who maps complete workflow trees for every system, user journey, and agent interaction — covering happy paths, all branch conditions, failure modes, recovery paths, handoff contracts, and observable states to produce build-ready specs that agents can implement against and QA can test against."
        ),
        AgencyAgent(
            id = "agency-workflow-optimizer",
            name = "Workflow Optimizer",
            slug = "agency-workflow-optimizer",
            division = "Product & Project",
            role = "Workflow Optimizer",
            description = "Expert process improvement specialist focused on analyzing, optimizing, and automating workflows across all business functions for maximum productivity and efficiency"
        ),
        AgencyAgent(
            id = "agency-x-twitter-intelligence-analyst",
            name = "X Twitter Intelligence Analyst",
            slug = "agency-x-twitter-intelligence-analyst",
            division = "Qualité & Tests",
            role = "X Twitter Intelligence Analyst",
            description = "Social intelligence specialist for X/Twitter research, trend detection, account monitoring, and evidence-backed audience insights using public signals and structured data workflows."
        ),
        AgencyAgent(
            id = "agency-xiaohongshu-specialist",
            name = "Xiaohongshu Specialist",
            slug = "agency-xiaohongshu-specialist",
            division = "Game Development",
            role = "Xiaohongshu Specialist",
            description = "Expert Xiaohongshu marketing specialist focused on lifestyle content, trend-driven strategies, and authentic community engagement. Masters micro-content creation and drives viral growth through aesthetic storytelling."
        ),
        AgencyAgent(
            id = "agency-xr-cockpit-interaction-specialist",
            name = "Xr Cockpit Interaction Specialist",
            slug = "agency-xr-cockpit-interaction-specialist",
            division = "Architecture & Systèmes",
            role = "Xr Cockpit Interaction Specialist",
            description = "Specialist in designing and developing immersive cockpit-based control systems for XR environments"
        ),
        AgencyAgent(
            id = "agency-xr-immersive-developer",
            name = "Xr Immersive Developer",
            slug = "agency-xr-immersive-developer",
            division = "Ingénierie & Dev",
            role = "Xr Immersive Developer",
            description = "Expert WebXR and immersive technology developer with specialization in browser-based AR/VR/XR applications"
        ),
        AgencyAgent(
            id = "agency-xr-interface-architect",
            name = "Xr Interface Architect",
            slug = "agency-xr-interface-architect",
            division = "Architecture & Systèmes",
            role = "Xr Interface Architect",
            description = "Spatial interaction designer and interface strategist for immersive AR/VR/XR environments"
        ),
        AgencyAgent(
            id = "agency-zhihu-strategist",
            name = "Zhihu Strategist",
            slug = "agency-zhihu-strategist",
            division = "Design & UI/UX",
            role = "Zhihu Strategist",
            description = "Expert Zhihu marketing specialist focused on thought leadership, community credibility, and knowledge-driven engagement. Masters question-answering strategy and builds brand authority through authentic expertise sharing."
        ),
        AgencyAgent(
            id = "agency-zk-steward",
            name = "Zk Steward",
            slug = "agency-zk-steward",
            division = "Design & UI/UX",
            role = "Zk Steward",
            description = "Knowledge-base steward in the spirit of Niklas Luhmann''s Zettelkasten. Default perspective: Luhmann; switches to domain experts (Feynman, Munger, Ogilvy, etc.) by task. Enforces atomic notes, connectivity, and validation loops. Use for knowledge-base building, note linking, complex task breakdown, and cross-domain decision support."
        ),
    )
}
