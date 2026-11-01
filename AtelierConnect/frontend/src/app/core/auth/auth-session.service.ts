import { Injectable, computed, signal } from '@angular/core';
import { LoginResponse as AdminLoginResponse } from '../../shared/models/auth.model';
import { ParticipantResponse, categorieParticipant } from '../../shared/models/participant.model';
import { LegacyParticipantCategory } from '../../shared/models/enums.model';
import { clearStoredValue, readStoredValue, writeStoredValue } from './session-storage.util';

const AUTH_SESSION_KEY = 'padel_auth_session';
const LEGACY_ADMIN_SESSION_KEY = 'padel_admin_session';
const LEGACY_MEMBER_SESSION_KEY = 'padel_member_session';

interface MemberSessionState {
  member: ParticipantResponse;
  token: string | null;
}

interface AuthSessionState {
  admin: AdminLoginResponse | null;
  member: MemberSessionState | null;
}

@Injectable({ providedIn: 'root' })
export class AuthSessionService {
  private readonly sessionState = signal<AuthSessionState | null>(this.loadFromStorage());

  readonly adminSession = computed(() => this.sessionState()?.admin ?? null);
  readonly adminToken = computed(() => this.sessionState()?.admin?.token ?? null);
  readonly adminRole = computed(() => this.sessionState()?.admin?.role ?? null);
  readonly adminCentreId = computed(() => this.sessionState()?.admin?.centreId ?? null);
  readonly memberSession = computed(() => this.sessionState()?.member?.member ?? null);
  readonly memberToken = computed(() => this.sessionState()?.member?.token ?? null);
  readonly memberId = computed(() => this.sessionState()?.member?.member.id ?? null);
  readonly memberMatricule = computed(() => this.sessionState()?.member?.member.matricule ?? null);
  readonly hasAdminSession = computed(() => !!this.adminToken());
  readonly hasMemberSession = computed(() => !!this.memberId());

  setAdminSession(session: AdminLoginResponse): void {
    this.persist({ ...this.currentState(), admin: session });
  }

  clearAdminSession(): void {
    this.persist({ ...this.currentState(), admin: null });
  }

  setMemberSession(member: ParticipantResponse, token?: string): void {
    this.persist({
      ...this.currentState(),
      member: { member, token: token ?? member.token ?? null }
    });
  }

  clearMemberSession(): void {
    this.persist({ ...this.currentState(), member: null });
  }

  clearAll(): void {
    this.persist({ admin: null, member: null });
  }

  private currentState(): AuthSessionState {
    return this.sessionState() ?? { admin: null, member: null };
  }

  private persist(nextState: AuthSessionState): void {
    const normalizedState: AuthSessionState = {
      admin: nextState.admin,
      member: nextState.member
    };

    if (!normalizedState.admin && !normalizedState.member) {
      this.sessionState.set(null);
      clearStoredValue(AUTH_SESSION_KEY);
      clearStoredValue(LEGACY_ADMIN_SESSION_KEY);
      clearStoredValue(LEGACY_MEMBER_SESSION_KEY);
      return;
    }

    this.sessionState.set(normalizedState);
    writeStoredValue(AUTH_SESSION_KEY, normalizedState);

    if (normalizedState.admin) {
      writeStoredValue(LEGACY_ADMIN_SESSION_KEY, normalizedState.admin);
    } else {
      clearStoredValue(LEGACY_ADMIN_SESSION_KEY);
    }

    if (normalizedState.member) {
      writeStoredValue(LEGACY_MEMBER_SESSION_KEY, normalizedState.member);
    } else {
      clearStoredValue(LEGACY_MEMBER_SESSION_KEY);
    }
  }

  private loadFromStorage(): AuthSessionState | null {
    const combined = readStoredValue<AuthSessionState>(AUTH_SESSION_KEY);
    if (combined && (combined.admin || combined.member)) {
      return {
        admin: combined.admin ? this.normalizeAdmin(combined.admin) : null,
        member: this.normalizeMemberSession(combined.member)
      };
    }

    const admin = readStoredValue<AdminLoginResponse>(LEGACY_ADMIN_SESSION_KEY);
    const member = this.readLegacyMemberSession();

    if (!admin && !member) {
      return null;
    }

    return {
      admin: admin ? this.normalizeAdmin(admin) : null,
      member
    };
  }

  private readLegacyMemberSession(): MemberSessionState | null {
    const parsed = readStoredValue<MemberSessionState | ParticipantResponse>(LEGACY_MEMBER_SESSION_KEY);
    return this.normalizeMemberSession(parsed);
  }

  private normalizeMemberSession(value: MemberSessionState | ParticipantResponse | null): MemberSessionState | null {
    if (!value) {
      return null;
    }

    if ('member' in value) {
      return {
        member: this.normalizeParticipant(value.member),
        token: value.token ?? value.member.token ?? null
      };
    }

    return {
      member: this.normalizeParticipant(value),
      token: value.token ?? null
    };
  }

  private normalizeAdmin(value: AdminLoginResponse): AdminLoginResponse {
    const legacy = value as AdminLoginResponse & { siteId?: number | null };
    return { ...value, centreId: value.centreId ?? legacy.siteId ?? null };
  }

  private normalizeParticipant(value: ParticipantResponse): ParticipantResponse {
    const legacy = value as ParticipantResponse & {
      siteId?: number | null;
      siteNom?: string | null;
      typeMembre?: LegacyParticipantCategory;
      typeParticipant?: LegacyParticipantCategory;
    };
    return {
      ...value,
      centreId: value.centreId ?? legacy.siteId ?? null,
      centreNom: value.centreNom ?? legacy.siteNom ?? null,
      categorie: categorieParticipant(legacy)
    };
  }
}
