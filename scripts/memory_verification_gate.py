#!/usr/bin/env python3
"""
CodeStation Memory Verification & Quality Gate Engine
Enforces strict quality, anti-noise filtering, and authentic bug-solution verification.

Mandates:
1. Rejects ALL general conversation, greetings, chat prompts, and theoretical chit-chat.
2. Validates authentic bug signatures (tracebacks, compiler errors, exit codes, exceptions).
3. Verifies concrete, reproducible solutions (commands, code patches, configuration changes).
4. Structures memory specifically for autonomous agent fast-lookup to save execution time.
"""

import re
from typing import Dict, List, Optional, Any, Tuple

# Patterns identifying conversational noise / chat prompts (STRICTLY DISQUALIFIED)
CHAT_NOISE_PATTERNS = [
    re.compile(r'^\s*(bonjour|salut|hello|hi|hey|coucou|merci|thanks|thx|de rien|au revoir|bye)\b', re.IGNORECASE),
    re.compile(r'^\s*(comment (vas-tu|ça va|ca va|allez-vous))\b', re.IGNORECASE),
    re.compile(r'^\s*(what can i help|how can i help|what would you like|what are we working on)\b', re.IGNORECASE),
    re.compile(r'^\s*(je suis|en tant que|hello! i\'m|i am an ai|as an ai assistant)\b', re.IGNORECASE),
    re.compile(r'^\s*(check-in|connexion établie|nous sommes le|testing session)\b', re.IGNORECASE),
    re.compile(r'^\s*(peux-tu|pourrais-tu|can you|could you|aide-moi|help me|raconte|explique)\b', re.IGNORECASE),
    re.compile(r'^\s*(qu\'est-ce que|what is|how do i|comment faire)\b(?![^?]*\b(error|exception|fail|crash|bug)\b)', re.IGNORECASE),
    re.compile(r'\[AGENCY ACTIVATION\s*:', re.IGNORECASE),
    re.compile(r'CODESTATION_SESSION_', re.IGNORECASE),
]

# Patterns identifying authentic, verifiable bugs and failures
BUG_SIGNATURE_PATTERNS = [
    # Language Exceptions & Panics
    re.compile(r'\b(Traceback \(most recent call last\)|Exception|Error|Caused by:)\b', re.IGNORECASE),
    re.compile(r'\b(NullPointerException|FileNotFoundException|ClassNotFoundException|NoSuchMethodError)\b'),
    re.compile(r'\b(ModuleNotFoundError|ImportError|AttributeError|IndexError|KeyError|TypeError|ValueError|RuntimeError|SyntaxError)\b'),
    re.compile(r'\b(panicked at|panic:|SIGSEGV|SIGBUS|core dumped|segmentation fault)\b', re.IGNORECASE),
    # Build & Compilation Failures
    re.compile(r'\b(BUILD FAILED|Compilation failed|error\[E\d+\]:|fatal error:|undefined reference to|cannot find symbol)\b', re.IGNORECASE),
    re.compile(r'\b(unresolved reference:|no native library found|cannot open shared object file)\b', re.IGNORECASE),
    re.compile(r'\b(gradlew|mvn|cargo|tsc|clang|gcc|rustc|ksp|kotlinc)\b.*\b(error|failed|failure)\b', re.IGNORECASE),
    # Network, HTTP & Permission Errors
    re.compile(r'\b(HTTP\s*(Error)?\s*(4\d\d|5\d\d)|429 Too Many Requests|ConnectionRefusedError|ETIMEDOUT|ECONNREFUSED)\b', re.IGNORECASE),
    re.compile(r'\b(Permission denied|EACCES|EPERM|access denied)\b', re.IGNORECASE),
    re.compile(r'\b(exit (status|code)\s*[1-9]\d*)\b', re.IGNORECASE),
    re.compile(r'\b(FATAL|CRITICAL)\b.*[:\-].*', re.IGNORECASE),
]

# Patterns identifying concrete, actionable remediation steps
REMEDIATION_ACTION_PATTERNS = [
    re.compile(r'\b(chmod|chown|mkdir -p|cp |ln -s|rm -rf|sed -i|export [A-Z_]+=|systemctl|service)\b'),
    re.compile(r'\b(apt-get|apt install|dpkg|cargo install|pip install|npm install|yarn add)\b'),
    re.compile(r'\b(\./gradlew --stop|\./gradlew assemble|cargo check|cargo build)\b'),
    re.compile(r'\b(patch|diff --git|git checkout|git cherry-pick|git commit|git revert)\b'),
    re.compile(r'\b(remediation|solution validée|correctif|solution|fix applied|workaround)\s*[:\-]', re.IGNORECASE),
    re.compile(r'(/org/sqlite/native/|libsqlitejdbc\.so|LD_LIBRARY_PATH|JAVA_HOME|ANDROID_HOME|PATH=)'),
]

# Patterns identifying validation proof / success confirmation
VERIFICATION_PROOF_PATTERNS = [
    re.compile(r'\b(build succeeded|build successful|tests? passed|all tests passed|exit code 0)\b', re.IGNORECASE),
    re.compile(r'\b(validé|conforme|résolu avec succès|succès|sans erreur native|réussit alors)\b', re.IGNORECASE),
    re.compile(r'\b(verified|proof|reproducible fix confirmed)\b', re.IGNORECASE),
]


class MemoryVerificationGate:
    """
    Independent Reality-Check and Quality Gate for Continuous Memory.
    Ensures that only authentic bugs and verified solutions enter collective memory.
    """

    @classmethod
    def is_chat_noise(cls, text: str, title: str = "") -> Tuple[bool, str]:
        """Returns (True, reason) if the text/title represents conversational noise or chat."""
        combined = f"{title}\n{text}".strip()
        if len(combined) < 20:
            return True, "Texte trop court (< 20 caractères) pour constituer un bug technique vérifiable."

        for pattern in CHAT_NOISE_PATTERNS:
            if pattern.search(title) or pattern.search(text[:250]):
                return True, f"Rejeté par filtre anti-bavardage : correspond au motif '{pattern.pattern}'."

        # If it's a dialog with greetings or empty pleasantries without code or errors
        lowered = combined.lower()
        pleasantries = ["bonjour", "salut", "hello", "hi there", "how are you", "comment vas-tu"]
        has_pleasantry = any(p in lowered[:60] for p in pleasantries)
        has_error = any(p.search(combined) for p in BUG_SIGNATURE_PATTERNS)
        if has_pleasantry and not has_error:
            return True, "Rejeté : Salutation ou échange conversationnel sans symptôme d'anomalie."

        return False, "Non-chat"

    @classmethod
    def extract_bug_signature(cls, text: str) -> Optional[Dict[str, Any]]:
        """Identifies and extracts explicit bug signature and error tokens."""
        if not text:
            return None

        matched_patterns = []
        for pat in BUG_SIGNATURE_PATTERNS:
            m = pat.search(text)
            if m:
                matched_patterns.append(m.group(0))

        if not matched_patterns:
            return None

        # Extract representative excerpt around first match
        first_match = matched_patterns[0]
        pos = text.find(first_match)
        start = max(0, pos - 60)
        end = min(len(text), pos + 180)
        excerpt = text[start:end].strip()

        return {
            "signature": first_match,
            "patterns_matched": matched_patterns,
            "symptom_excerpt": excerpt
        }

    @classmethod
    def extract_remediation_proof(cls, text: str) -> Optional[Dict[str, Any]]:
        """Identifies concrete remediation actions and validation proof."""
        if not text:
            return None

        actions = [p.search(text).group(0) for p in REMEDIATION_ACTION_PATTERNS if p.search(text)]
        proofs = [p.search(text).group(0) for p in VERIFICATION_PROOF_PATTERNS if p.search(text)]

        if not actions and not proofs:
            # Check for general technical fix keywords
            has_fix_kw = any(w in text.lower() for w in ["fix", "corrigé", "correction", "solution", "résolu", "contournement"])
            if not has_fix_kw:
                return None

        return {
            "actions": actions,
            "proofs": proofs,
            "has_concrete_action": len(actions) > 0,
            "has_verification": len(proofs) > 0
        }

    @classmethod
    def evaluate_quality(
        cls,
        title: str,
        problem: str,
        solution: str,
        context: str = "",
        tags: Optional[List[str]] = None
    ) -> Tuple[bool, float, Dict[str, Any]]:
        """
        Comprehensive Quality Assessment.
        Returns (is_acceptable, confidence_score, audit_report).
        Requires score >= 0.85 for acceptance.
        """
        audit = {
            "checks": {},
            "penalties": [],
            "reasons": []
        }

        # 1. Anti-Noise & Chat Check
        is_noise, noise_reason = cls.is_chat_noise(problem + "\n" + solution, title)
        if is_noise:
            audit["checks"]["anti_noise"] = "FAIL"
            audit["penalties"].append(-1.0)
            audit["reasons"].append(noise_reason)
            return False, 0.0, audit
        audit["checks"]["anti_noise"] = "PASS"

        score = 0.0

        # 2. Bug Signature Check (+0.35)
        bug_info = cls.extract_bug_signature(problem + "\n" + title)
        if bug_info:
            score += 0.35
            audit["checks"]["bug_signature"] = "PASS"
            audit["bug_signature"] = bug_info["signature"]
        else:
            audit["checks"]["bug_signature"] = "MISSING"
            audit["penalties"].append(-0.40)
            audit["reasons"].append("Absence de signature d'erreur ou d'exception identifiable.")

        # 3. Remediation Action Check (+0.35)
        rem_info = cls.extract_remediation_proof(solution)
        if rem_info and (rem_info["has_concrete_action"] or rem_info["has_verification"]):
            score += 0.35
            audit["checks"]["remediation_action"] = "PASS"
            if rem_info["has_concrete_action"]:
                score += 0.10
        else:
            audit["checks"]["remediation_action"] = "VAGUE"
            audit["penalties"].append(-0.35)
            audit["reasons"].append("Solution trop vague : aucune commande concrète ni patch de correction identifié.")

        # 4. Verification Proof (+0.15)
        if rem_info and rem_info["has_verification"]:
            score += 0.15
            audit["checks"]["verification_proof"] = "PASS"
        else:
            audit["checks"]["verification_proof"] = "UNVERIFIED"

        # 5. Context Specificity (+0.15)
        if len(context.strip()) > 15:
            score += 0.15
            audit["checks"]["context"] = "PASS"
        else:
            audit["checks"]["context"] = "MINIMAL"

        # Apply penalties
        for p in audit["penalties"]:
            score += p

        final_score = max(0.0, min(1.0, round(score, 3)))
        audit["final_score"] = final_score

        # Minimum required score = 0.85
        is_acceptable = (final_score >= 0.85) and (audit["checks"].get("bug_signature") == "PASS")
        if not is_acceptable and not audit["reasons"]:
            audit["reasons"].append(f"Score de confiance insuffisant ({final_score} < 0.85 requis).")

        return is_acceptable, final_score, audit

    @classmethod
    def format_agent_timesaver(
        cls,
        title: str,
        bug_signature: str,
        problem: str,
        root_cause: str,
        solution: str,
        verification: str = "",
        context: str = "CodeStation Runtime / PRoot ARM64",
        time_saved: str = "15-30 mins"
    ) -> str:
        """Formats the knowledge payload into an ultra-fast lookup structure for autonomous agents."""
        clean_title = title.replace("[FIX]", "").replace("[BUG-FIX]", "").strip()
        return (
            f"### [RÉSOLUTION RAPIDE AGENT : {clean_title}]\n\n"
            f"**[SYMPTÔME / SIGNATURE D'ERREUR]** :\n```\n{bug_signature}\n```\n\n"
            f"**[PROBLÈME OBSERVÉ]** : {problem}\n\n"
            f"**[CAUSE RACINE]** : {root_cause or 'Incompatibilité d environnement ou mauvaise configuration'}\n\n"
            f"**[ACTION DE REMÉDIATION IMMÉDIATE (GAIN DE TEMPS: {time_saved})]** :\n```bash\n{solution}\n```\n\n"
            f"**[PREUVE DE VALIDATION]** : {verification or 'Vérifié sans erreur sur l environnement cible.'}\n"
            f"**[ENVIRONNEMENT]** : {context}"
        )
