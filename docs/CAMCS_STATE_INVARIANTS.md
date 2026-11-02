# Invariants d'État et Contrat de Données Unique CAMCS v2.0
**CodeStation Autonomous Multi-Agent Collaboration System**
*Garanties Formelles de Non-Régression & Découplage Android / Backend*

---

## 1. Principe de la Source Unique de Vérité (SSOT)

Le backend Python (`camcs_engine.py`) et sa base relationnelle SQLite (`/root/.codestation/camcs.db`) constituent l'unique et absolue source de vérité pour l'ensemble du système CodeStation.

L'application Android native :
- Ne recalcule **JAMAIS** un pourcentage d'avancement global en faisant la moyenne des agents.
- N'infère **JAMAIS** un statut de mission à partir des statuts individuels des agents.
- Ne déduit **JAMAIS** la phase d'orchestration à partir de seuils de progression arbitraires.
- Consomme les données via le contrat REST (`/api/camcs/missions`) et le flux SSE temps réel (`/api/camcs/events`).

---

## 2. Invariants d'État Formels

### Invariant 1 : Règle de Complétion Parfaite
$$\text{Status}(\text{Mission}) = \text{COMPLETED} \iff \left( \text{Progress}(\text{Mission}) = 100 \land \forall t \in \text{Tasks}_{\text{mandatory}}, \text{Status}(t) = \text{COMPLETED} \right)$$
*Conséquence* : Il est mathématiquement impossible pour une mission d'être marquée `COMPLETED` si la progression est à 75%, 99% ou si une tâche obligatoire est en attente, en erreur ou annulée.

### Invariant 2 : Vivacité et Réalité de l'État RUNNING
$$\text{Status}(\text{Agent}) = \text{RUNNING} \iff \left( \text{PID} \in \text{Processes}_{\text{active}} \lor \text{Request}_{\text{api\_active}} = \text{TRUE} \right)$$
*Conséquence* : Un agent dont le processus est terminé ne peut pas demeurer en `RUNNING`.

### Invariant 3 : Exclusion Réciproque d'Attente et de Complétion
$$\text{Progress}(\text{Agent}) = 100 \implies \text{Status}(\text{Agent}) \neq \text{WAITING\_FOR\_AGENT} \land \text{Status}(\text{Agent}) \neq \text{WAITING\_FOR\_DEPENDENCY}$$
*Conséquence* : Un agent ayant achevé l'ensemble de ses attributions passe immédiatement en `VALIDATING` puis `COMPLETED`.

### Invariant 4 : Immuabilité des États Terminaux
$$\forall s \in \{ \text{COMPLETED}, \text{FAILED}, \text{CANCELLED} \}, \quad \text{Transition}(s, s') = \text{REJECTED} \quad (\forall s')$$
*Conséquence* : Une mission ou un agent ayant atteint un état terminal est gelé. Aucune mise à jour concurrente ne peut le réactiver.

### Invariant 5 : Disponibilité Universelle de l'Annulation
$$\forall s \notin \{ \text{COMPLETED}, \text{FAILED}, \text{CANCELLED} \}, \quad \text{CanCancel}(s) = \text{TRUE}$$
*Conséquence* : L'utilisateur dispose en permanence du pouvoir d'interruption sur n'importe quelle mission non terminée, y compris lors des phases amont (`PLANNING`, `DISPATCHING`).

### Invariant 6 : Ségrégation des Métriques Visuelles
$$\text{Agents Actifs} = \sum_{a \in \text{Agents}} \mathbb{I}(\text{Status}(a) \in \{ \text{STARTING}, \text{RUNNING}, \text{STREAMING} \})$$
$$\text{Agents Terminés} = \sum_{a \in \text{Agents}} \mathbb{I}(\text{Status}(a) = \text{COMPLETED})$$
*Conséquence* : L'interface affiche distinctement les agents en cours d'exécution et les agents ayant achevé leur tâche, supprimant le paradoxe visuel "0/4 agents actifs alors que plusieurs cartes affichent 100%".

---

## 3. Contrat de Données JSON (REST & SSE)

```typescript
interface CamcsMissionPayload {
    id: string;                      // UUID unique
    title: string;                   // Titre de la mission
    objective: string;               // Objectif utilisateur en langage naturel
    governanceMode: "AUTONOMOUS" | "SEMI_AUTONOMOUS" | "HYBRID" | "SUPERVISED";
    status: MissionState;            // État issu de la machine d'état stricte
    progress: number;                // Entier 0 à 100 (calculé par pondération)
    orchestrationPhase: string;      // Phase réelle déterminée par le backend
    createdAt: number;               // Timestamp Unix (secondes)
    updatedAt: number;
    completedAt: number | null;
    activeAgentsCount: number;       // Compteur dynamique d'agents réellement actifs
    totalAgentsCount: number;        // Nombre total d'agents engagés dans la mission
    agents: CamcsAgentPayload[];
    tasks: CamcsTaskPayload[];
    deliverables: CamcsDeliverablePayload[];
}
```

Ce contrat est strictement implémenté côté Android dans `app/src/main/java/com/ghdinteractivestudio/codestation/domain/model/CamcsModels.kt`.
