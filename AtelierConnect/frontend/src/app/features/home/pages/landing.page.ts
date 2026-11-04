import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AteliersApiService } from '../../../core/api/ateliers-api.service';
import { MemberSessionService } from '../../../core/auth/member-session.service';
import { LanguageService } from '../../../core/i18n/language.service';

@Component({
  selector: 'app-landing-page',
  standalone: true,
  imports: [CommonModule, RouterLink, MatButtonModule, MatCardModule, MatProgressSpinnerModule],
  templateUrl: './landing.page.html',
  styleUrl: './landing.page.scss',
})
export class LandingPage {
  readonly editorial = computed(() =>
    this.language.locale() === 'fr'
      ? {
          eyebrow: 'Des moments à créer, des liens à tisser',
          headline: 'Faites place à votre',
          accent: 'créativité.',
          introduction:
            'Une table, des idées, de belles rencontres. Réservez votre prochain atelier et retrouvez le plaisir de faire ensemble.',
          explore: 'Trouver mon prochain atelier',
          discover: 'Découvrir l’expérience',
          note: 'Les plus belles idées prennent vie ensemble.',
          duration: 'pour déconnecter',
          people: 'participants maximum',
          price: 'par participant',
          collectionLabel: 'Un peu de temps pour vous',
          collectionTitle: 'Plus qu’un atelier, un moment à partager.',
          collectionBody:
            'Débutant curieux ou passionné, venez comme vous êtes. Le reste se crée ensemble.',
          card1: 'Le plaisir du fait main',
          card2: 'Des idées qui prennent couleur',
          card3: 'Un espace pour se retrouver',
          card1Body: 'Ralentir, expérimenter et donner forme à ses idées.',
          card2Body: 'Oser essayer et laisser parler sa créativité.',
          card3Body: 'S’installer autour d’une table et partager un vrai moment.',
          stepsLabel: 'Simple, du début à la fin',
          stepsTitle: 'Votre parenthèse créative, en trois temps.',
          step1: 'Trouvez votre moment',
          step1Body: 'Un centre, une salle et un créneau qui vous ressemblent.',
          step2: 'Réservez votre place',
          step2Body: 'Rejoignez un atelier public ou invitez vos proches en privé.',
          step3: 'Vivez l’expérience',
          step3Body: 'Retrouvez vos réservations et vos paiements au même endroit.',
          cta: 'Et si votre prochaine belle rencontre commençait ici ?',
          ctaBody: 'Du temps pour soi. De la place pour les autres.',
          photoAlt:
            'Quatre adultes créent des objets en céramique autour d’une table dans un atelier lumineux',
          paintingAlt: 'Une séance de peinture à l’aquarelle autour d’une table',
          studioAlt: 'Un atelier créatif lumineux avec une grande table en bois',
          availability: 'Consulter les ateliers publics',
          availabilityBody: 'Les disponibilités de votre communauté, en un instant.',
        }
      : {
          eyebrow: 'Make something lovely. Meet someone new.',
          headline: 'Make room for your',
          accent: 'creativity.',
          introduction:
            'A shared table, fresh ideas, meaningful connections. Book your next workshop and rediscover the joy of making together.',
          explore: 'Find my next workshop',
          discover: 'Discover the experience',
          note: 'The best ideas come to life together.',
          duration: 'to switch off',
          people: 'participants maximum',
          price: 'per participant',
          collectionLabel: 'A little time for yourself',
          collectionTitle: 'More than a workshop. A moment to share.',
          collectionBody:
            'Curious beginner or lifelong enthusiast, come as you are. We will make the rest together.',
          card1: 'The joy of handmade',
          card2: 'Ideas in full colour',
          card3: 'A space to come together',
          card1Body: 'Slow down, experiment, and give your ideas a little shape.',
          card2Body: 'Try something new and let your creativity wander.',
          card3Body: 'Gather around a table and share a real moment.',
          stepsLabel: 'Simple from start to finish',
          stepsTitle: 'Your creative escape, in three steps.',
          step1: 'Find your moment',
          step1Body: 'Choose a centre, a room, and a time that suits you.',
          step2: 'Save your place',
          step2Body: 'Join a public workshop or invite your friends to a private one.',
          step3: 'Enjoy the experience',
          step3Body: 'Keep your bookings and payments together in one place.',
          cta: 'What if your next great connection started here?',
          ctaBody: 'A little time for you. A little room for others.',
          photoAlt: 'Four adults making ceramics around a shared table in a sunlit creative studio',
          paintingAlt: 'A shared watercolour painting session',
          studioAlt: 'A light-filled creative studio with a large wooden table',
          availability: 'Check public workshops',
          availabilityBody: 'See what your community has to offer.',
        },
  );
  private readonly ateliersApi = inject(AteliersApiService);
  readonly memberSession = inject(MemberSessionService);
  private readonly language = inject(LanguageService);

  readonly loading = signal(false);
  readonly count = signal<number | null>(null);
  readonly errorCode = signal<'api' | ''>('');
  readonly message = computed(() => {
    if (this.count() === null) {
      return '';
    }

    return this.language.t('landing.apiSuccess', { count: this.count() ?? 0 });
  });
  readonly error = computed(() => {
    if (!this.errorCode()) {
      return '';
    }

    return this.language.t('landing.apiError');
  });
  readonly copy = computed(() => ({
    eyebrow: this.language.t('landing.eyebrow'),
    title: this.language.t('landing.title'),
    lead: this.language.t('landing.lead'),
    memberCta: this.language.t('landing.memberCta'),
    memberCtaConnected: this.language.t('landing.memberCtaConnected'),
    adminCta: this.language.t('landing.adminCta'),
    testApi: this.language.t('landing.testApi'),
    loadingApi: this.language.t('landing.loadingApi'),
    atelierPreviewLabel: this.language.t('landing.atelierPreviewLabel'),
    atelierPublic: this.language.t('landing.atelierPublic'),
    atelierAvailable: this.language.t('landing.atelierAvailable'),
    reservation: this.language.t('landing.reservation'),
    reservationBody: this.language.t('landing.reservationBody'),
    simpleReservation: this.language.t('landing.simpleReservation'),
    feature1Title: this.language.t('landing.feature1.title'),
    feature1Body: this.language.t('landing.feature1.body'),
    feature2Title: this.language.t('landing.feature2.title'),
    feature2Body: this.language.t('landing.feature2.body'),
    feature3Title: this.language.t('landing.feature3.title'),
    feature3Body: this.language.t('landing.feature3.body'),
    feature4Title: this.language.t('landing.feature4.title'),
    feature4Body: this.language.t('landing.feature4.body'),
  }));

  checkApi(): void {
    this.loading.set(true);
    this.errorCode.set('');

    this.ateliersApi.getPublic().subscribe({
      next: (ateliers) => {
        this.count.set(ateliers.length);
        this.loading.set(false);
      },
      error: () => {
        this.count.set(null);
        this.errorCode.set('api');
        this.loading.set(false);
      },
    });
  }
}
