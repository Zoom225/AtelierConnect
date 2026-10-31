export type Locale = 'fr' | 'en';

export type TranslationKey =
  | 'nav.home'
  | 'nav.admin'
  | 'nav.member'
  | 'nav.profile'
  | 'nav.matches'
  | 'nav.reservations'
  | 'nav.payments'
  | 'nav.create'
  | 'nav.public'
  | 'nav.private'
  | 'nav.logout'
  | 'nav.memberArea'
  | 'nav.adminLogin'
  | 'nav.dashboard'
  | 'nav.members'
  | 'nav.sites'
  | 'nav.courts'
  | 'nav.closures'
  | 'common.back'
  | 'common.save'
  | 'common.create'
  | 'common.edit'
  | 'common.delete'
  | 'common.reset'
  | 'common.cancel'
  | 'common.confirm'
  | 'common.loading'
  | 'common.none'
  | 'common.matchPlanned'
  | 'common.matchFull'
  | 'common.matchCanceled'
  | 'common.reservationPending'
  | 'common.reservationConfirmed'
  | 'common.reservationCanceled'
  | 'common.paymentPending'
  | 'common.paymentPaid'
  | 'common.paymentRefunded'
  | 'common.paymentCanceled'
  | 'common.allSites'
  | 'common.profile'
  | 'common.refresh'
  | 'switcher.label'
  | 'switcher.fr'
  | 'switcher.en'
  | 'landing.eyebrow'
  | 'landing.title'
  | 'landing.lead'
  | 'landing.memberCta'
  | 'landing.memberCtaConnected'
  | 'landing.adminCta'
  | 'landing.testApi'
  | 'landing.loadingApi'
  | 'landing.apiSuccess'
  | 'landing.apiError'
  | 'landing.feature1.title'
  | 'landing.feature1.body'
  | 'landing.feature2.title'
  | 'landing.feature2.body'
  | 'landing.feature3.title'
  | 'landing.feature3.body'
  | 'landing.feature4.title'
  | 'landing.feature4.body'
  | 'landing.matchPublic'
  | 'landing.matchAvailable'
  | 'landing.reservation'
  | 'landing.reservationBody'
  | 'landing.simpleReservation'
  | 'landing.matchPreviewLabel'
  | 'admin.login.heroTitle'
  | 'admin.login.heroSubtitle'
  | 'admin.login.formTitle'
  | 'admin.login.formSubtitle'
  | 'admin.login.email'
  | 'admin.login.password'
  | 'admin.login.submit'
  | 'admin.login.back'
  | 'admin.login.error'
  | 'admin.home.eyebrow'
  | 'admin.home.title'
  | 'admin.home.subtitle'
  | 'admin.home.loading'
  | 'admin.home.matches'
  | 'admin.home.reservations'
  | 'admin.home.members'
  | 'admin.home.revenue'
  | 'admin.home.occupancy'
  | 'admin.home.occupancySub'
  | 'admin.home.resources'
  | 'admin.home.resourcesSub'
  | 'admin.home.empty'
  | 'admin.home.waiting'
  | 'admin.home.visible'
  | 'admin.home.validPayments'
  | 'admin.home.reservationsLoadError'
  | 'admin.home.dashboardLoadError'
  | 'admin.matches.title'
  | 'admin.matches.subtitle'
  | 'admin.members.title'
  | 'admin.members.subtitle'
  | 'admin.members.matricule'
  | 'admin.members.matriculeHint'
  | 'admin.members.type'
  | 'admin.members.typeGlobal'
  | 'admin.members.typeSite'
  | 'admin.members.typeLibre'
  | 'admin.members.nom'
  | 'admin.members.prenom'
  | 'admin.members.email'
  | 'admin.members.site'
  | 'admin.members.siteNone'
  | 'admin.members.searchPlaceholder'
  | 'admin.members.filterAll'
  | 'admin.members.filterGlobal'
  | 'admin.members.filterSite'
  | 'admin.members.filterLibre'
  | 'admin.members.empty'
  | 'admin.members.emptySub'
  | 'admin.members.penalty'
  | 'admin.members.emailMissing'
  | 'admin.members.siteAll'
  | 'admin.members.balance'
  | 'admin.members.edit'
  | 'admin.members.delete'
  | 'admin.members.count'
  | 'admin.sites.title'
  | 'admin.sites.subtitle'
  | 'admin.terrains.title'
  | 'admin.terrains.subtitle'
  | 'admin.closures.title'
  | 'admin.closures.subtitle'
  | 'member.home.title'
  | 'member.home.subtitle'
  | 'member.home.identification'
  | 'member.home.loginHint'
  | 'member.home.password'
  | 'member.home.cta'
  | 'member.home.invalid'
  | 'member.home.global'
  | 'member.home.site'
  | 'member.home.livre'
  | 'member.home.tousSites'
  | 'member.create.title'
  | 'member.create.subtitle'
  | 'member.create.rappel'
  | 'member.create.public'
  | 'member.create.private'
  | 'member.create.back'
  | 'member.create.submit'
  | 'member.create.mode'
  | 'member.create.site'
  | 'member.create.terrain'
  | 'member.create.date'
  | 'member.create.dateHint'
  | 'member.create.time'
  | 'member.create.matchType'
  | 'member.create.privatePlayers'
  | 'member.create.player2'
  | 'member.create.player3'
  | 'member.create.player4'
  | 'member.create.player5'
  | 'member.create.player6'
  | 'member.create.noSite'
  | 'member.create.noTerrain'
  | 'member.create.selectedSite'
  | 'member.create.bookingLimit'
  | 'member.create.bookingLimitProfileError'
  | 'member.create.ruleGlobal'
  | 'member.create.ruleSite'
  | 'member.create.ruleLibre'
  | 'member.create.noSiteLoaded'
  | 'member.create.noTerrainLoaded'
  | 'member.create.success'
  | 'member.create.successPrivate'
  | 'member.create.partialSuccess'
  | 'member.public.title'
  | 'member.public.subtitle'
  | 'member.public.filter'
  | 'member.public.search'
  | 'member.public.create'
  | 'member.public.join'
  | 'member.public.modify'
  | 'member.public.delete'
  | 'member.public.myProfile'
  | 'member.public.myMatch'
  | 'member.public.available'
  | 'member.public.completion'
  | 'member.public.none'
  | 'member.public.organizer'
  | 'member.public.status'
  | 'member.public.type'
  | 'member.public.players'
  | 'member.public.price'
  | 'member.public.registering'
  | 'member.public.editTitle'
  | 'member.public.editHint'
  | 'member.public.noMatches'
  | 'member.public.searchPlaceholder'
  | 'member.public.noMatchPrompt'
  | 'member.public.createBody'
  | 'member.public.date'
  | 'member.public.time'
  | 'member.public.typeLabel'
  | 'member.reservations.title'
  | 'member.reservations.subtitle'
  | 'member.reservations.createPublic'
  | 'member.reservations.createPrivate'
  | 'member.reservations.matches'
  | 'member.reservations.select'
  | 'member.reservations.addPlayer'
  | 'member.reservations.noOrganized'
  | 'member.reservations.edit'
  | 'member.reservations.confirmation'
  | 'member.reservations.players'
  | 'member.reservations.empty'
  | 'member.reservations.player'
  | 'member.reservations.match'
  | 'member.reservations.reservation'
  | 'member.reservations.payment'
  | 'member.reservations.amount'
  | 'member.reservations.pay'
  | 'member.reservations.cancel'
  | 'member.reservations.organizedTitle'
  | 'member.reservations.organizedSub'
  | 'member.reservations.chooseMatch'
  | 'member.reservations.inviteMatricule'
  | 'member.reservations.consentInfo'
  | 'member.reservations.consentAction'
  | 'member.reservations.consentConfirm'
  | 'member.reservations.consentSuccess'
  | 'member.reservations.consentError'
  | 'member.reservations.noPlayer'
  | 'member.reservations.reservationLabel'
  | 'member.reservations.paymentLabel'
  | 'member.reservations.removePlayer'
  | 'member.reservations.emptyBody'
  | 'member.payments.title'
  | 'member.payments.subtitle'
  | 'member.payments.reservations'
  | 'member.payments.total'
  | 'member.payments.paid'
  | 'member.payments.pending'
  | 'member.payments.refunded'
  | 'member.payments.none'
  | 'member.payments.notSet'
  | 'member.profile.title'
  | 'member.profile.subtitle'
  | 'member.profile.createQuick'
  | 'member.profile.publicMatch'
  | 'member.profile.privateMatch'
  | 'member.profile.quickPublic'
  | 'member.profile.quickPrivate'
  | 'member.profile.quickPublicBody'
  | 'member.profile.quickPrivateBody'
  | 'member.profile.explore'
  | 'member.profile.followup'
  | 'member.profile.finance'
  | 'member.profile.refresh'
  | 'member.profile.logout'
  | 'member.profile.nom'
  | 'member.profile.prenom'
  | 'member.profile.email'
  | 'member.profile.matricule'
  | 'member.profile.type'
  | 'member.profile.site'
  | 'member.profile.solde'
  | 'member.profile.penaltyActive'
  | 'member.profile.balancePending'
  | 'member.profile.yes'
  | 'member.profile.no'
  | 'member.profile.exploreBody'
  | 'member.profile.followupBody'
  | 'member.profile.financeBody'
  | 'member.payments.payment'
  | 'member.payments.loading'
  | 'member.payments.error'
  | 'member.payments.noMember'
  | 'member.payments.idLabel'
  | 'member.payments.noPayments'
  | 'member.payments.loadingState'
  | 'member.payments.errorState'
  | 'member.payments.noMemberState'
  | 'admin.matches.organizer'
  | 'admin.matches.status'
  | 'admin.matches.players'
  | 'admin.matches.reservations'
  | 'admin.matches.noReservations'
  | 'admin.matches.hideDetails'
  | 'admin.matches.showDetails'
  | 'admin.matches.convert'
  | 'admin.matches.convertSuccess'
  | 'admin.matches.loadError'
  | 'admin.matches.reservationsError'
  | 'admin.matches.convertError'
  | 'admin.matches.empty'
  | 'admin.closures.date'
  | 'admin.closures.reason'
  | 'admin.closures.site'
  | 'admin.closures.noneGlobal'
  | 'admin.closures.global'
  | 'admin.closures.add'
  | 'admin.closures.reset'
  | 'admin.closures.reasonEmpty'
  | 'admin.closures.empty'
  | 'common.notProvided'
  | 'admin.home.allStatuses'
  | 'admin.home.matchCount'
  | 'admin.home.sitesVisible'
  | 'admin.home.terrainsVisible'
  | 'admin.home.completeMatches'
  | 'admin.home.pendingReservations'
  | 'admin.login.globalBadge'
  | 'admin.login.siteBadge'
  | 'admin.login.showPassword'
  | 'admin.login.hidePassword'
  | 'admin.members.searchNoResult'
  | 'admin.members.loadError'
  | 'admin.members.saveError'
  | 'admin.members.deleteError'
  | 'admin.members.updateSuccess'
  | 'admin.members.createSuccess'
  | 'admin.members.deleteSuccess'
  | 'admin.members.deleteConfirm'
  | 'admin.sites.name'
  | 'admin.sites.address'
  | 'admin.sites.opening'
  | 'admin.sites.closing'
  | 'admin.sites.matchDuration'
  | 'admin.sites.breakDuration'
  | 'admin.sites.civilYear'
  | 'admin.sites.openVisitors'
  | 'admin.sites.hours'
  | 'admin.sites.duration'
  | 'admin.sites.break'
  | 'admin.sites.year'
  | 'admin.sites.empty'
  | 'admin.sites.siteAdminEditBlocked'
  | 'admin.sites.siteAdminDeleteBlocked'
  | 'admin.sites.loadError'
  | 'admin.sites.saveError'
  | 'admin.sites.deleteError'
  | 'admin.sites.deleteConfirm'
  | 'admin.sites.updateSuccess'
  | 'admin.sites.createSuccess'
  | 'admin.sites.deleteSuccess'
  | 'admin.terrains.name'
  | 'admin.terrains.site'
  | 'admin.terrains.empty'
  | 'admin.terrains.loadError'
  | 'admin.terrains.saveError'
  | 'admin.terrains.deleteError'
  | 'admin.terrains.deleteConfirm'
  | 'admin.terrains.updateSuccess'
  | 'admin.terrains.createSuccess'
  | 'admin.terrains.deleteSuccess'
  | 'admin.closures.loadError'
  | 'admin.closures.addSuccess'
  | 'admin.closures.addError'
  | 'admin.closures.deleteSuccess'
  | 'admin.closures.deleteError'
  | 'member.home.matricule'
  | 'member.home.matriculePlaceholder'
  | 'member.home.showPassword'
  | 'member.home.hidePassword'
  | 'member.home.globalHint'
  | 'member.home.siteHint'
  | 'member.home.livreHint'
  | 'member.home.allSites'
  | 'member.profile.loadError'
  | 'member.create.bookingLimitError'
  | 'member.create.invalidType'
  | 'member.create.createError'
  | 'member.create.outstandingBalance'
  | 'member.create.activePenalty'
  | 'member.create.slotTaken'
  | 'member.create.siteClosed'
  | 'member.create.outOfHours'
  | 'member.create.siteOnly'
  | 'member.create.internalError'
  | 'member.create.addPlayersError'
  | 'member.public.loadError'
  | 'member.public.noMember'
  | 'member.public.joinBlocked'
  | 'member.public.joinSuccess'
  | 'member.public.joinError'
  | 'member.public.modifyTooLate'
  | 'member.public.updateSuccess'
  | 'member.public.updateError'
  | 'member.public.deleteConfirm'
  | 'member.public.deleteSuccess'
  | 'member.public.deleteError'
  | 'member.reservations.reservationTitle'
  | 'member.reservations.noMember'
  | 'member.reservations.loadError'
  | 'member.reservations.privateOnly'
  | 'member.reservations.playersLoadError'
  | 'member.reservations.updateSuccess'
  | 'member.reservations.updateError'
  | 'member.reservations.deleteSuccess'
  | 'member.reservations.deleteError'
  | 'member.reservations.playerAdded'
  | 'member.reservations.playerAddError'
  | 'member.reservations.playerRemoved'
  | 'member.reservations.playerRemoveError'
  | 'member.reservations.paymentSuccess'
  | 'member.reservations.paymentError'
  | 'member.reservations.reservationCanceled'
  | 'member.reservations.reservationCancelError';

export type TranslationParams = Record<string, string | number>;

export const TRANSLATIONS: Record<Locale, Partial<Record<TranslationKey, string>>> = {
  fr: {
    'nav.home': 'Accueil',
    'nav.admin': 'Admin',
    'nav.member': 'Espace participant',
    'nav.profile': 'Profil',
    'nav.matches': 'Ateliers',
    'nav.reservations': 'Reservations',
    'nav.payments': 'Paiements',
    'nav.create': 'Creer',
    'nav.public': 'Public',
    'nav.private': 'Prive',
    'nav.logout': 'Deconnexion',
    'nav.memberArea': 'Espace participant',
    'nav.adminLogin': 'Connexion admin',
    'nav.dashboard': 'Dashboard',
    'nav.members': 'Participants',
    'nav.sites': 'Centres',
    'nav.courts': 'Salles',
    'nav.closures': 'Fermetures',
    'common.back': 'Retour',
    'common.save': 'Enregistrer',
    'common.create': 'Creer',
    'common.edit': 'Modifier',
    'common.delete': 'Supprimer',
    'common.reset': 'Reinitialiser',
    'common.cancel': 'Annuler',
    'common.confirm': 'Confirmer',
    'common.loading': 'Chargement...',
    'common.none': 'Aucun',
    'common.matchPlanned': 'Planifie',
    'common.matchFull': 'Complet',
    'common.matchCanceled': 'Annule',
    'common.reservationPending': 'En attente',
    'common.reservationConfirmed': 'Confirmee',
    'common.reservationCanceled': 'Annulee',
    'common.paymentPending': 'En attente',
    'common.paymentPaid': 'Paye',
    'common.paymentRefunded': 'Rembourse',
    'common.paymentCanceled': 'Annule',
    'common.allSites': 'Tous les centres',
    'common.profile': 'Profil',
    'common.refresh': 'Rafraichir',
    'switcher.label': 'Choix de langue',
    'switcher.fr': 'Francais',
    'switcher.en': 'English',
    'landing.eyebrow': 'Gestion et réservation d’ateliers collaboratifs multi-centres',
    'landing.title': 'AtelierConnect',
    'landing.lead':
      'Une interface claire pour reserver, rejoindre un match, suivre les paiements et piloter les sites depuis un tableau de bord admin.',
    'landing.memberCta': 'Entrer avec un matricule',
    'landing.memberCtaConnected': 'Ouvrir mon espace',
    'landing.adminCta': 'Connexion admin',
    'landing.testApi': "Tester l'API",
    'landing.loadingApi': 'Verification de /api/matches/public...',
    'landing.apiSuccess': 'Proxy OK - {count} atelier(s) public(s) recupere(s)',
    'landing.apiError': "Echec de l'appel API. Verifiez que le backend tourne sur le port 8082.",
    'landing.feature1.title': 'Reservations',
    'landing.feature1.body': 'Choix de la salle, contrôle des disponibilités et suivi du statut.',
    'landing.feature2.title': 'Ateliers publics et prives',
    'landing.feature2.body': 'Creation rapide avec des regles claires pour les participants.',
    'landing.feature3.title': 'Paiements',
    'landing.feature3.body': 'Vue propre des paiements, penalites et confirmations.',
    'landing.feature4.title': 'Administration',
    'landing.feature4.body': 'Indicateurs, participants, centres, salles et fermetures au meme endroit.',
    'landing.matchPublic': 'Atelier public',
    'landing.matchAvailable': 'atelier(s) disponible(s)',
    'landing.reservation': 'Reservation',
    'landing.reservationBody': 'participant, salle, paiement',
    'landing.simpleReservation': 'Simple',
    'landing.matchPreviewLabel': 'Apercu des ateliers',
    'admin.login.heroTitle': 'AtelierConnect Administration',
    'admin.login.heroSubtitle': 'Espace reserve aux administrateurs de centres et superviseurs globaux',
    'admin.login.formTitle': 'Connexion',
    'admin.login.formSubtitle': 'Acces reserve aux administrateurs',
    'admin.login.email': 'Email',
    'admin.login.password': 'Mot de passe',
    'admin.login.submit': 'Se connecter',
    'admin.login.back': 'Retour',
    'admin.login.error': "Echec de connexion. Verifiez l'e-mail et le mot de passe.",
    'admin.home.eyebrow': 'Tableau de bord',
    'admin.home.title': 'Administration AtelierConnect',
    'admin.home.subtitle': 'Vue {scope} des ateliers, reservations, participants et ressources du centre.',
    'admin.home.loading': 'Chargement du tableau de bord...',
    'admin.home.matches': 'Ateliers',
    'admin.home.reservations': 'Reservations',
    'admin.home.members': 'Participants',
    'admin.home.revenue': "Chiffre d'affaires",
    'admin.home.occupancy': 'Occupation par centre',
    'admin.home.occupancySub': 'Nombre d’ateliers rattachés à chaque centre visible',
    'admin.home.resources': 'Ressources',
    'admin.home.resourcesSub': "Controle rapide des elements geres par l'administration",
    'admin.home.empty': 'Aucune donnee.',
    'admin.home.waiting': 'en attente',
    'admin.home.visible': 'visibles',
    'admin.home.validPayments': 'paiements valides',
    'admin.home.reservationsLoadError': 'Impossible de charger les reservations administrateur.',
    'admin.home.dashboardLoadError': 'Impossible de charger le tableau de bord administrateur.',
    'admin.matches.title': 'Gestion des ateliers',
    'admin.matches.subtitle': 'Consultation, details et conversion en public',
    'admin.members.title': 'Gestion des participants',
    'admin.members.subtitle': 'Création, modification et gestion des participants aux ateliers',
    'admin.members.matricule': 'Matricule',
    'admin.members.matriculeHint': 'PARTENAIRE : G + 4 chiffres · ABONNÉ : S + 5 chiffres · VISITEUR : L + 5 chiffres',
    'admin.members.type': 'Catégorie de participant',
    'admin.members.typeGlobal': 'PARTENAIRE — jusqu’à 30 jours',
    'admin.members.typeSite': 'ABONNÉ — jusqu’à 15 jours dans son centre',
    'admin.members.typeLibre': 'VISITEUR — jusqu’à 7 jours',
    'admin.members.nom': 'Nom',
    'admin.members.prenom': 'Prenom',
    'admin.members.email': 'Email',
    'admin.members.site': 'Centre',
    'admin.members.siteNone': 'Aucun centre de rattachement (partenaire / visiteur)',
    'admin.members.searchPlaceholder': 'Rechercher par matricule, nom ou prenom...',
    'admin.members.filterAll': 'Tous',
    'admin.members.filterGlobal': 'Partenaires',
    'admin.members.filterSite': 'Abonnés',
    'admin.members.filterLibre': 'Visiteurs',
    'admin.members.empty': 'Aucun participant trouve',
    'admin.members.emptySub': 'Creer votre premier participant avec le formulaire ci-dessus.',
    'admin.members.penalty': 'Pénalité active',
    'admin.members.emailMissing': 'Non renseigne',
    'admin.members.siteAll': 'Tous les centres',
    'admin.members.balance': 'Solde',
    'admin.members.edit': 'Modifier',
    'admin.members.delete': 'Supprimer',
    'admin.members.count': 'participant(s)',
    'admin.sites.title': 'Gestion des centres',
    'admin.sites.subtitle': 'Creation, edition et suppression des centres',
    'admin.terrains.title': 'Gestion des salles',
    'admin.terrains.subtitle': 'Creation et gestion des salles par centre',
    'admin.closures.title': 'Jours de fermeture',
    'admin.closures.subtitle': 'Gestion des fermetures globales et par centre',
    'member.home.title': 'Espace participant AtelierConnect',
    'member.home.subtitle': 'Connectez-vous pour consulter et réserver vos ateliers',
    'member.home.identification': 'Identification',
    'member.home.loginHint': 'Saisissez votre matricule et votre mot de passe pour accéder aux ateliers',
    'member.home.password': 'Mot de passe',
    'member.home.cta': 'Acceder a mon espace',
    'member.home.invalid': 'Identifiants invalides. Verifiez le matricule et le mot de passe.',
    'member.home.global': 'PARTENAIRE',
    'member.home.site': 'ABONNÉ',
    'member.home.livre': 'VISITEUR',
    'member.home.tousSites': 'Tous les centres',
    'member.create.title': 'Créer un atelier',
    'member.create.subtitle': 'Public ou prive, selon les regles du backend.',
    'member.create.rappel': 'Rappel',
    'member.create.public': 'PUBLIC',
    'member.create.private': 'PRIVE',
    'member.create.back': 'Retour profil',
    'member.create.submit': 'Créer l’atelier',
    'member.create.mode': 'Mode creation',
    'member.create.site': 'Centre',
    'member.create.terrain': 'Salle',
    'member.create.date': 'Date',
    'member.create.dateHint': 'Entre',
    'member.create.time': 'Heure debut',
    'member.create.matchType': 'Type d’atelier',
    'member.create.privatePlayers': 'Participants à inviter (jusqu’à 5, facultatif)',
    'member.create.player2': 'Matricule participant 2',
    'member.create.player3': 'Matricule participant 3',
    'member.create.player4': 'Matricule participant 4',
    'member.create.player5': 'Matricule participant 5',
    'member.create.player6': 'Matricule participant 6',
    'member.create.noSite': 'Aucun centre disponible pour créer un atelier.',
    'member.create.noTerrain': 'Aucune salle disponible dans le centre sélectionné.',
    'member.create.selectedSite': 'Centre selectionne',
    'member.create.bookingLimit': 'Derniere date autorisee selon votre profil :',
    'member.create.ruleGlobal': 'PARTENAIRE : réservation jusqu\'à 30 jours à l\'avance',
    'member.create.ruleSite': 'ABONNÉ : réservation jusqu\'à 15 jours dans son centre',
    'member.create.ruleLibre': 'VISITEUR : réservation jusqu\'à 7 jours dans les centres ouverts aux visiteurs',
    'member.create.noSiteLoaded': 'Aucun centre charge.',
    'member.create.noTerrainLoaded': 'Aucun salle charge.',
    'member.create.success': 'Atelier créé. Votre place sera confirmée après paiement depuis vos réservations.',
    'member.create.successPrivate': 'Atelier privé créé. Les invitations sont en attente de paiement des participants.',
    'member.create.partialSuccess': "Match cree, mais certains joueurs n'ont pas pu etre ajoutes.",
    'member.public.title': 'Ateliers ouverts au public',
    'member.public.subtitle': 'Inscrivez-vous à un atelier disponible. Les places sont attribuées après paiement.',
    'member.public.filter': 'Filtrer par centre',
    'member.public.search': 'Recherche',
    'member.public.create': 'Creer un atelier',
    'member.public.join': 'Rejoindre',
    'member.public.modify': 'Modifier',
    'member.public.delete': 'Supprimer',
    'member.public.myProfile': 'Mon profil',
    'member.public.myMatch': 'Mon atelier',
    'member.public.available': 'Disponible',
    'member.public.completion': 'Complet',
    'member.public.none': 'Non rejoignable',
    'member.public.organizer': 'Organisateur',
    'member.public.status': 'Statut',
    'member.public.type': 'Type',
    'member.public.players': 'Participants',
    'member.public.price': 'Prix par participant',
    'member.public.registering': 'Reservation...',
    'member.public.editTitle': 'Modifier le atelier',
    'member.public.editHint': 'Modification indisponible à moins de 24 h avant l’atelier.',
    'member.public.noMatches': 'Aucun atelier public ne correspond aux filtres.',
    'member.public.searchPlaceholder': 'Salle, centre, organisateur',
    'member.public.noMatchPrompt': 'Tu ne trouves pas de atelier ?',
    'member.public.createBody': 'Atelier public ou privé, avec choix du centre et de la salle.',
    'member.public.date': 'Date',
    'member.public.time': 'Heure debut',
    'member.public.typeLabel': 'Type',
    'member.reservations.title': 'Mes reservations',
    'member.reservations.subtitle': 'Paiement et annulation de vos inscriptions.',
    'member.reservations.createPublic': 'Créer un atelier PUBLIC',
    'member.reservations.createPrivate': 'Créer un atelier PRIVÉ',
    'member.reservations.matches': 'Ateliers publics',
    'member.reservations.select': 'Selectionner un atelier organise',
    'member.reservations.addPlayer': 'Inviter un participant',
    'member.reservations.noOrganized': 'Aucun atelier organise pour l\'instant. Creez un atelier PUBLIC ou PRIVE via les boutons ci-dessus.',
    'member.reservations.edit': 'Modifier le atelier',
    'member.reservations.confirmation': 'Confirmation requise',
    'member.reservations.players': 'Participants inscrits :',
    'member.reservations.empty': 'Aucune reservation trouvee.',
    'member.reservations.player': 'Participant',
    'member.reservations.match': 'Atelier',
    'member.reservations.reservation': 'Reservation',
    'member.reservations.payment': 'Paiement',
    'member.reservations.amount': 'Montant',
    'member.reservations.pay': 'Payer',
    'member.reservations.cancel': 'Annuler',
    'member.reservations.organizedTitle': 'Mes ateliers organises',
    'member.reservations.organizedSub': 'Modifier, supprimer ou gerer les participants de vos ateliers.',
    'member.reservations.chooseMatch': '-- Choisir un atelier --',
    'member.reservations.inviteMatricule': 'Matricule du participant à inviter',
    'member.reservations.consentInfo': 'Si cet atelier privé reste incomplet, il ne pourra être ouvert au public à J−2 qu’avec votre accord explicite.',
    'member.reservations.consentAction': 'J’autorise l’ouverture au public à J−2',
    'member.reservations.consentConfirm': 'Confirmez-vous votre accord pour ouvrir cet atelier au public à partir de J−2 s’il reste incomplet ? Cette décision sera enregistrée.',
    'member.reservations.consentSuccess': 'Votre accord pour une éventuelle ouverture au public est enregistré.',
    'member.reservations.consentError': 'Impossible d’enregistrer votre accord.'
    'member.reservations.noPlayer': 'Aucun participant inscrit sur ce atelier.',
    'member.reservations.reservationLabel': 'Reservation',
    'member.reservations.paymentLabel': 'Paiement',
    'member.reservations.removePlayer': 'Retirer',
    'member.reservations.emptyBody': 'Creez un atelier ou rejoignez un atelier public pour voir vos reservations ici.',
    'member.payments.title': 'Mes paiements',
    'member.payments.subtitle': 'Historique des paiements et remboursements',
    'member.payments.reservations': 'Mes reservations',
    'member.payments.total': 'Total paiements',
    'member.payments.paid': 'Total paye',
    'member.payments.pending': 'En attente',
    'member.payments.refunded': 'Rembourses',
    'member.payments.none': 'Aucun paiement trouve',
    'member.payments.notSet': 'Pas encore regle',
    'member.payments.idLabel': 'Paiement',
    'member.payments.noPayments': 'Aucun paiement trouve',
    'member.payments.loadingState': 'Chargement des paiements...',
    'member.payments.errorState': 'Impossible de charger les paiements.',
    'member.payments.noMemberState': 'Aucun participant connecte.',
    'member.profile.title': 'Mon profil participant',
    'member.profile.subtitle': 'Informations rechargees depuis le backend',
    'member.profile.createQuick': 'Creer un atelier rapidement',
    'member.profile.publicMatch': 'Atelier ouvert',
    'member.profile.privateMatch': 'Atelier sur invitation',
    'member.profile.quickPublic': 'Creer un atelier PUBLIC',
    'member.profile.quickPrivate': 'Creer un atelier PRIVE',
    'member.profile.quickPublicBody': "N'importe quel membre peut rejoindre · 15 EUR/joueur · 4 joueurs requis",
    'member.profile.quickPrivateBody': 'Invitez jusqu’à 5 participants · ouverture au public à J-2 avec accord explicite',
    'member.profile.explore': 'Explorer',
    'member.profile.followup': 'Suivi',
    'member.profile.finance': 'Finances',
    'member.profile.refresh': 'Rafraichir',
    'member.profile.logout': 'Deconnexion participant',
    'member.profile.nom': 'Nom',
    'member.profile.prenom': 'Prenom',
    'member.profile.email': 'Email',
    'member.profile.matricule': 'Matricule',
    'member.profile.type': 'Type',
    'member.profile.site': 'Centre',
    'member.profile.solde': 'Solde',
    'member.profile.penaltyActive': 'Penalite active',
    'member.profile.balancePending': 'Solde en attente',
    'member.profile.yes': 'Oui',
    'member.profile.no': 'Non',
    'member.profile.exploreBody': 'Rejoins rapidement une partie disponible.',
    'member.profile.followupBody': 'Paie, annule ou suis tes inscriptions.',
    'member.profile.financeBody': 'Historique de tes paiements.',
    'member.payments.payment': 'Paiement',
    'member.payments.loading': 'Chargement des paiements...',
    'member.payments.error': 'Impossible de charger les paiements.',
    'member.payments.noMember': 'Aucun participant connecte.',
    'admin.matches.organizer': 'Organisateur',
    'admin.matches.status': 'Statut',
    'admin.matches.players': 'Participants',
    'admin.matches.reservations': 'Reservations du atelier',
    'admin.matches.noReservations': 'Aucune reservation.',
    'admin.matches.hideDetails': 'Masquer',
    'admin.matches.showDetails': 'Voir details',
    'admin.matches.convert': 'Ouvrir au public (accord requis, J−2)',
    'admin.matches.convertSuccess': 'Atelier converti en public.',
    'admin.matches.loadError': 'Impossible de charger les ateliers admin.',
    'admin.matches.reservationsError': 'Impossible de charger les reservations du atelier.',
    'admin.matches.convertError': 'Conversion impossible.',
    'admin.matches.empty': 'Aucun atelier disponible',
    'admin.closures.date': 'Date',
    'admin.closures.reason': 'Raison (optionnel)',
    'admin.closures.site': 'Centre concerne',
    'admin.closures.noneGlobal': 'Aucun (global)',
    'admin.closures.global': 'Fermeture globale (tous les centres)',
    'admin.closures.add': 'Ajouter la fermeture',
    'admin.closures.reset': 'Reinitialiser',
    'admin.closures.reasonEmpty': 'Raison non precisee',
    'admin.closures.empty': 'Aucun jour de fermeture configure',
    'common.notProvided': 'Non renseigne',
    'admin.home.allStatuses': 'tous statuts',
    'admin.home.matchCount': 'atelier(s)',
    'admin.home.sitesVisible': 'Centres visibles',
    'admin.home.terrainsVisible': 'Salles visibles',
    'admin.home.completeMatches': 'Ateliers complets',
    'admin.home.pendingReservations': 'Reservations en attente',
    'admin.login.globalBadge': '🌐 Admin global',
    'admin.login.siteBadge': '🏟️ Admin centre',
    'admin.login.showPassword': 'Afficher le mot de passe admin',
    'admin.login.hidePassword': 'Masquer le mot de passe admin',
    'admin.members.searchNoResult': 'Aucun resultat pour "{query}"',
    'admin.members.loadError': 'Impossible de charger les participants.',
    'admin.members.saveError': 'Sauvegarde du participant impossible.',
    'admin.members.deleteError': 'Suppression impossible.',
    'admin.members.updateSuccess': 'Participant mis à jour.',
    'admin.members.createSuccess': 'Participant cree avec succes.',
    'admin.members.deleteSuccess': 'Participant supprimé.',
    'admin.members.deleteConfirm': 'Confirmer la suppression de ce participant ?',
    'admin.sites.name': 'Nom',
    'admin.sites.address': 'Adresse',
    'admin.sites.opening': 'Ouverture',
    'admin.sites.closing': 'Fermeture',
    'admin.sites.matchDuration': 'Duree atelier (min)',
    'admin.sites.breakDuration': 'Pause entre ateliers (min)',
    'admin.sites.civilYear': 'Année civile',
    'admin.sites.openVisitors': 'Centre ouvert aux visiteurs (réservation à 7 jours)',
    'admin.sites.hours': 'Horaires',
    'admin.sites.duration': 'Duree atelier',
    'admin.sites.break': 'Pause',
    'admin.sites.year': 'Annee',
    'admin.sites.empty': 'Aucun centre configure',
    'admin.sites.siteAdminEditBlocked': 'Un admin SITE ne peut pas modifier les centres.',
    'admin.sites.siteAdminDeleteBlocked': 'Un admin SITE ne peut pas supprimer de centre.',
    'admin.sites.loadError': 'Impossible de charger les centres.',
    'admin.sites.saveError': 'Sauvegarde du centre impossible.',
    'admin.sites.deleteError': 'Suppression du centre impossible.',
    'admin.sites.deleteConfirm': 'Confirmer la suppression de ce centre ?',
    'admin.sites.updateSuccess': 'Centre mis a jour.',
    'admin.sites.createSuccess': 'Centre cree.',
    'admin.sites.deleteSuccess': 'Centre supprime.',
    'admin.terrains.name': 'Nom de la salle',
    'admin.terrains.site': 'Centre',
    'admin.terrains.empty': 'Aucun salle configure',
    'admin.terrains.loadError': 'Impossible de charger les salles.',
    'admin.terrains.saveError': 'Enregistrement de la salle impossible.',
    'admin.terrains.deleteError': 'Suppression de la salle impossible.',
    'admin.terrains.deleteConfirm': 'Confirmer la suppression de cette salle ?',
    'admin.terrains.updateSuccess': 'Salle mis a jour.',
    'admin.terrains.createSuccess': 'Salle cree.',
    'admin.terrains.deleteSuccess': 'Salle supprime.',
    'admin.closures.loadError': 'Impossible de charger les jours de fermeture.',
    'admin.closures.addSuccess': 'Jour de fermeture ajoute.',
    'admin.closures.addError': 'Ajout impossible.',
    'admin.closures.deleteSuccess': 'Jour de fermeture supprime.',
    'admin.closures.deleteError': 'Suppression impossible.',
    'member.home.matricule': 'Matricule',
    'member.home.matriculePlaceholder': 'Ex: G1234, S12345, L12345',
    'member.home.showPassword': 'Afficher le mot de passe participant',
    'member.home.hidePassword': 'Masquer le mot de passe participant',
    'member.home.globalHint': 'G + 4 chiffres · réservation à 30 jours',
    'member.home.siteHint': 'S + 5 chiffres · réservation à 15 jours dans votre centre',
    'member.home.livreHint': 'L + 5 chiffres · réservation à 7 jours',
    'member.home.allSites': 'Tous les centres',
    'member.profile.loadError': 'Impossible de charger le profil participant.',
    'member.create.bookingLimitError': 'Dernière date autorisée : {date}.',
    'member.create.bookingLimitProfileError': 'Le profil {type} ne peut pas réserver plus de {days} jours à l\'avance. Date limite : {date}.',
    'member.create.invalidType': 'Type de atelier invalide.',
    'member.create.createError': 'Creation du atelier impossible.',
    'member.create.outstandingBalance': 'Vous avez un solde impaye. Reglez vos dettes avant de creer un atelier.',
    'member.create.activePenalty': 'Vous avez une penalite active. Attendez avant de creer un atelier.',
    'member.create.slotTaken': 'Ce creneau est deja reserve. Choisissez un autre horaire.',
    'member.create.siteClosed': 'Le centre est ferme a cette date.',
    'member.create.outOfHours': "Cet horaire est en dehors des heures d'ouverture du site.",
    'member.create.siteOnly': 'Un abonné ne peut créer un atelier que dans son centre de rattachement.',
    'member.create.internalError': 'Une erreur interne du serveur est survenue lors de la creation du atelier.',
    'member.create.addPlayersError': "Impossible d'ajouter tous les joueurs.",
    'member.public.loadError': 'Impossible de charger les ateliers publics.',
    'member.public.noMember': 'Aucun participant connecte.',
    'member.public.joinBlocked': 'Ce atelier ne peut pas etre rejoint (complet, annule ou non autorise).',
    'member.public.joinSuccess': "Demande créée. Votre place ne sera confirmée qu'après paiement.",
    'member.public.joinError': 'Impossible de reserver ce atelier.',
    'member.public.modifyTooLate': 'Modification impossible a moins de 24 h du debut du atelier.',
    'member.public.updateSuccess': 'Atelier modifie avec succes.',
    'member.public.updateError': 'Modification du atelier impossible.',
    'member.public.deleteConfirm': 'Confirmer la suppression (annulation) du atelier #{id} ?',
    'member.public.deleteSuccess': 'Atelier annule avec succes.',
    'member.public.deleteError': 'Suppression du atelier impossible.',
    'member.reservations.reservationTitle': 'Reservation',
    'member.reservations.noMember': 'Aucun participant connecte.',
    'member.reservations.loadError': 'Impossible de charger les reservations.',
    'member.reservations.privateOnly': 'La gestion des participants est réservée aux ateliers PRIVÉS.',
    'member.reservations.playersLoadError': 'Impossible de charger les participants du atelier.',
    'member.reservations.updateSuccess': 'Atelier mis à jour avec succès.',
    'member.reservations.updateError': 'Modification du atelier impossible.',
    'member.reservations.deleteSuccess': 'Atelier annule avec succes.',
    'member.reservations.deleteError': 'Suppression du atelier impossible.',
    'member.reservations.playerAdded': 'Participant ajoute avec succes.',
    'member.reservations.playerAddError': "Ajout du joueur impossible.",
    'member.reservations.playerRemoved': 'Participant retire du atelier.',
    'member.reservations.playerRemoveError': 'Suppression du participant impossible.',
    'member.reservations.paymentSuccess': 'Paiement effectue avec succes.',
    'member.reservations.paymentError': 'Paiement impossible.',
    'member.reservations.reservationCanceled': 'Reservation annulee.',
    'member.reservations.reservationCancelError': 'Annulation impossible.'
  },
  en: {
    'common.back': 'Back',
    'common.save': 'Save',
    'common.create': 'Create',
    'common.edit': 'Edit',
    'common.delete': 'Delete',
    'common.reset': 'Reset',
    'common.cancel': 'Cancel',
    'common.confirm': 'Confirm',
    'common.loading': 'Loading...',
    'common.none': 'None',
    'common.matchPlanned': 'Planned',
    'common.matchFull': 'Full',
    'common.matchCanceled': 'Canceled',
    'common.reservationPending': 'Pending',
    'common.reservationConfirmed': 'Confirmed',
    'common.reservationCanceled': 'Canceled',
    'common.paymentPending': 'Pending',
    'common.paymentPaid': 'Paid',
    'common.paymentRefunded': 'Refunded',
    'common.paymentCanceled': 'Canceled',
    'common.allSites': 'All centers',
    'common.profile': 'Profile',
    'common.refresh': 'Refresh',
    'nav.home': 'Home',
    'nav.admin': 'Admin',
    'nav.member': 'Participant area',
    'nav.profile': 'Profile',
    'nav.matches': 'Workshops',
    'nav.reservations': 'Reservations',
    'nav.payments': 'Payments',
    'nav.create': 'Create',
    'nav.public': 'Public',
    'nav.private': 'Private',
    'nav.logout': 'Logout',
    'nav.memberArea': 'Participant area',
    'nav.adminLogin': 'Admin login',
    'nav.dashboard': 'Dashboard',
    'nav.members': 'Participants',
    'nav.sites': 'Centers',
    'nav.courts': 'Workshop rooms',
    'nav.closures': 'Closures',
    'switcher.label': 'Language switch',
    'switcher.fr': 'French',
    'switcher.en': 'English',
    'landing.eyebrow': 'Collaborative workshop booking across multiple centers',
    'landing.title': 'AtelierConnect',
    'landing.lead':
      'A clear interface to book, join a match, track payments, and run the club from an admin dashboard.',
    'landing.memberCta': 'Enter with a participant ID',
    'landing.memberCtaConnected': 'Open my area',
    'landing.adminCta': 'Admin login',
    'landing.testApi': 'Test the API',
    'landing.loadingApi': 'Checking /api/workshops/public...',
    'landing.apiSuccess': 'Proxy OK - {count} public workshop(es) fetched',
    'landing.apiError': 'API call failed. Check that the backend is running on port 8082.',
    'landing.feature1.title': 'Reservations',
    'landing.feature1.body': 'Workshop room selection, availability checks, and status tracking.',
    'landing.feature2.title': 'Public and private workshops',
    'landing.feature2.body': 'Fast creation with clear rules for participants.',
    'landing.feature3.title': 'Payments',
    'landing.feature3.body': 'Clean view of payments, penalties, and confirmations.',
    'landing.feature4.title': 'Administration',
    'landing.feature4.body': 'Metrics, participants, centers, rooms, and closures in one place.',
    'landing.matchPublic': 'Public workshop',
    'landing.matchAvailable': 'workshop(es) available',
    'landing.reservation': 'Reservation',
    'landing.reservationBody': 'participant, workshop room, payment',
    'landing.simpleReservation': 'Simple',
    'landing.matchPreviewLabel': 'Workshop preview',
    'admin.login.heroTitle': 'AtelierConnect Administration',
    'admin.login.heroSubtitle': 'Area reserved for center administrators and global supervisors',
    'admin.login.formTitle': 'Login',
    'admin.login.formSubtitle': 'Access reserved for administrators',
    'admin.login.email': 'Email',
    'admin.login.password': 'Password',
    'admin.login.submit': 'Sign in',
    'admin.login.back': 'Back',
    'admin.login.error': 'Login failed. Check the email and password.',
    'admin.home.eyebrow': 'Dashboard',
    'admin.home.title': 'AtelierConnect Administration',
    'admin.home.subtitle': '{scope} view of center workshops, reservations, participants, and resources.',
    'admin.home.loading': 'Loading dashboard...',
    'admin.home.matches': 'Workshops',
    'admin.home.reservations': 'Reservations',
    'admin.home.members': 'Participants',
    'admin.home.revenue': 'Revenue',
    'admin.home.occupancy': 'Occupancy by center',
    'admin.home.occupancySub': 'Number of workshops linked to each visible center',
    'admin.home.resources': 'Resources',
    'admin.home.resourcesSub': 'Quick control of elements managed by the administration',
    'admin.home.empty': 'No data.',
    'admin.home.waiting': 'waiting',
    'admin.home.visible': 'visible',
    'admin.home.validPayments': 'valid payments',
    'admin.home.reservationsLoadError': 'Unable to load admin reservations.',
    'admin.home.dashboardLoadError': 'Unable to load the admin dashboard.',
    'admin.matches.title': 'Workshop management',
    'admin.matches.subtitle': 'Review, details, and conversion to public',
    'admin.members.title': 'Participant management',
    'admin.members.subtitle': 'Create, edit and manage workshop participants',
    'admin.members.matricule': 'Participant ID',
    'admin.members.matriculeHint': 'PARTNER: G + 4 digits · SUBSCRIBER: S + 5 digits · VISITOR: L + 5 digits',
    'admin.members.type': 'Participant category',
    'admin.members.typeGlobal': 'PARTNER — book up to 30 days ahead',
    'admin.members.typeSite': 'SUBSCRIBER — book up to 15 days at your center',
    'admin.members.typeLibre': 'VISITOR — book up to 7 days ahead',
    'admin.members.nom': 'Last name',
    'admin.members.prenom': 'First name',
    'admin.members.email': 'Email',
    'admin.members.site': 'Center',
    'admin.members.siteNone': 'No assigned center (partner / visitor)',
    'admin.members.searchPlaceholder': 'Search by participant ID, last name, or first name...',
    'admin.members.filterAll': 'All',
    'admin.members.filterGlobal': 'Partners',
    'admin.members.filterSite': 'Subscribers',
    'admin.members.filterLibre': 'Visitors',
    'admin.members.empty': 'No participant found',
    'admin.members.emptySub': 'Create your first participant with the form above.',
    'admin.members.penalty': 'Active penalty',
    'admin.members.emailMissing': 'Not set',
    'admin.members.siteAll': 'All centers',
    'admin.members.balance': 'Balance',
    'admin.members.edit': 'Edit',
    'admin.members.delete': 'Delete',
    'admin.members.count': 'participant(s)',
    'admin.sites.title': 'Center management',
    'admin.sites.subtitle': 'Create, edit, and delete centers',
    'admin.terrains.title': 'Room management',
    'admin.terrains.subtitle': 'Create and manage rooms by center',
    'admin.closures.title': 'Closure days',
    'admin.closures.subtitle': 'Manage global and center-specific closure days',
    'member.home.title': 'AtelierConnect participant area',
    'member.home.subtitle': 'Sign in to explore and book workshops',
    'member.home.identification': 'Identification',
    'member.home.loginHint': 'Enter your participant ID and password to access workshops',
    'member.home.password': 'Password',
    'member.home.cta': 'Open my area',
    'member.home.invalid': 'Invalid credentials. Check the participant ID and password.',
    'member.home.global': 'PARTNER',
    'member.home.site': 'SUBSCRIBER',
    'member.home.livre': 'VISITOR',
    'member.home.tousSites': 'All centers',
    'member.create.title': 'Create a workshop',
    'member.create.subtitle': 'Public or private, according to backend rules.',
    'member.create.rappel': 'Reminder',
    'member.create.public': 'PUBLIC',
    'member.create.private': 'PRIVATE',
    'member.create.back': 'Back to profile',
    'member.create.submit': 'Create workshop',
    'member.create.mode': 'Creation mode',
    'member.create.site': 'Center',
    'member.create.terrain': 'Workshop room',
    'member.create.date': 'Date',
    'member.create.dateHint': 'Between',
    'member.create.time': 'Start time',
    'member.create.matchType': 'Workshop type',
    'member.create.privatePlayers': 'Participants to invite (up to 5, optional)',
    'member.create.player2': 'Participant ID 2',
    'member.create.player3': 'Participant ID 3',
    'member.create.player4': 'Participant ID 4',
    'member.create.player5': 'Participant ID 5',
    'member.create.player6': 'Participant ID 6',
    'member.create.noSite': 'No center available for a new workshop.',
    'member.create.noTerrain': 'No room available at the selected center.',
    'member.create.selectedSite': 'Selected center',
    'member.create.bookingLimit': 'Latest allowed date according to your profile:',
    'member.create.ruleGlobal': 'PARTNER: up to 30 days in advance',
    'member.create.ruleSite': 'SUBSCRIBER: up to 15 days at your assigned center',
    'member.create.ruleLibre': 'VISITOR: up to 7 days at centers open to visitors',
    'member.create.noSiteLoaded': 'No center loaded.',
    'member.create.noTerrainLoaded': 'No workshop room loaded.',
    'member.create.success': 'Workshop created. Your place is confirmed after payment.',
    'member.create.successPrivate': 'Private workshop created. Invitations await participant payments.',
    'member.create.partialSuccess': 'Workshop created, but some participants could not be added.',
    'member.public.title': 'Open workshops',
    'member.public.subtitle': 'Register for an available workshop. Places are confirmed after payment.',
    'member.public.filter': 'Filter by center',
    'member.public.search': 'Search',
    'member.public.create': 'Create a workshop',
    'member.public.join': 'Join',
    'member.public.modify': 'Edit',
    'member.public.delete': 'Delete',
    'member.public.myProfile': 'My profile',
    'member.public.myMatch': 'My workshop',
    'member.public.available': 'Available',
    'member.public.completion': 'Full',
    'member.public.none': 'Not joinable',
    'member.public.organizer': 'Organizer',
    'member.public.status': 'Status',
    'member.public.type': 'Type',
    'member.public.players': 'Participants',
    'member.public.price': 'Price per participant',
    'member.public.registering': 'Booking...',
    'member.public.editTitle': 'Edit workshop',
    'member.public.editHint': 'Editing is unavailable less than 24 hours before the workshop.',
    'member.public.noMatches': 'No public workshop workshops the filters.',
    'member.public.searchPlaceholder': 'Workshop room, center, organizer',
    'member.public.noMatchPrompt': 'Can\'t find a workshop?',
    'member.public.createBody': 'Public or private, with center and workshop room choice.',
    'member.public.date': 'Date',
    'member.public.time': 'Start time',
    'member.public.typeLabel': 'Type',
    'member.reservations.title': 'My reservations',
    'member.reservations.subtitle': 'Payment and cancellation of your registrations.',
    'member.reservations.createPublic': 'Create PUBLIC workshop',
    'member.reservations.createPrivate': 'Create PRIVATE workshop',
    'member.reservations.matches': 'Public workshops',
    'member.reservations.select': 'Select an organized workshop',
    'member.reservations.addPlayer': 'Invite a participant',
    'member.reservations.noOrganized': 'No organized workshop yet. Create a PUBLIC or PRIVATE workshop using the buttons above.',
    'member.reservations.edit': 'Edit workshop',
    'member.reservations.confirmation': 'Confirmation required',
    'member.reservations.players': 'Registered participants:',
    'member.reservations.empty': 'No reservation found.',
    'member.reservations.player': 'Participant',
    'member.reservations.match': 'Workshop',
    'member.reservations.reservation': 'Reservation',
    'member.reservations.payment': 'Payment',
    'member.reservations.amount': 'Amount',
    'member.reservations.pay': 'Pay',
    'member.reservations.cancel': 'Cancel',
    'member.reservations.organizedTitle': 'My organized workshops',
    'member.reservations.organizedSub': 'Edit, delete, or manage participants for your workshops.',
    'member.reservations.chooseMatch': '-- Choose a workshop --',
    'member.reservations.inviteMatricule': 'Participant ID to invite',
    'member.reservations.consentInfo': 'If this private workshop is not full, it can open to the public at D−2 only with your explicit consent.',
    'member.reservations.consentAction': 'I authorize public opening at D−2',
    'member.reservations.consentConfirm': 'Do you agree to open this workshop to the public from D−2 if it remains incomplete? Your decision will be recorded.',
    'member.reservations.consentSuccess': 'Your consent to possible public opening has been recorded.',
    'member.reservations.consentError': 'Unable to record your consent.'
    'member.reservations.noPlayer': 'No participant registered for this workshop.',
    'member.reservations.reservationLabel': 'Reservation',
    'member.reservations.paymentLabel': 'Payment',
    'member.reservations.removePlayer': 'Remove',
    'member.reservations.emptyBody': 'Create a workshop or join a public workshop to see your reservations here.',
    'member.payments.title': 'My payments',
    'member.payments.subtitle': 'Payment and refund history',
    'member.payments.reservations': 'My reservations',
    'member.payments.total': 'Total payments',
    'member.payments.paid': 'Total paid',
    'member.payments.pending': 'Pending',
    'member.payments.refunded': 'Refunded',
    'member.payments.none': 'No payment found',
    'member.payments.notSet': 'Not set yet',
    'member.payments.idLabel': 'Payment',
    'member.payments.noPayments': 'No payment found',
    'member.payments.loadingState': 'Loading payments...',
    'member.payments.errorState': 'Unable to load payments.',
    'member.payments.noMemberState': 'No participant logged in.',
    'member.profile.title': 'My participant profile',
    'member.profile.subtitle': 'Information loaded from the backend',
    'member.profile.createQuick': 'Create a workshop quickly',
    'member.profile.publicMatch': 'Open workshop',
    'member.profile.privateMatch': 'Invitation workshop',
    'member.profile.quickPublic': 'Create a PUBLIC workshop',
    'member.profile.quickPrivate': 'Create a PRIVATE workshop',
    'member.profile.quickPublicBody': 'Individual booking · €15 per participant · maximum 6 places',
    'member.profile.quickPrivateBody': 'Invite up to 5 participants · public opening at D-2 with explicit consent',
    'member.profile.explore': 'Explore',
    'member.profile.followup': 'Follow-up',
    'member.profile.finance': 'Finance',
    'member.profile.refresh': 'Refresh',
    'member.profile.logout': 'Logout participant',
    'member.profile.nom': 'Last name',
    'member.profile.prenom': 'First name',
    'member.profile.email': 'Email',
    'member.profile.matricule': 'Participant ID',
    'member.profile.type': 'Type',
    'member.profile.site': 'Center',
    'member.profile.solde': 'Balance',
    'member.profile.penaltyActive': 'Active penalty',
    'member.profile.balancePending': 'Pending balance',
    'member.profile.yes': 'Yes',
    'member.profile.no': 'No',
    'member.profile.exploreBody': 'Join an available workshop quickly.',
    'member.profile.followupBody': 'Pay, cancel, or track your registrations.',
    'member.profile.financeBody': 'Payment history.',
    'member.payments.payment': 'Payment',
    'member.payments.loading': 'Loading payments...',
    'member.payments.error': 'Unable to load payments.',
    'member.payments.noMember': 'No participant logged in.',
    'admin.matches.organizer': 'Organizer',
    'admin.matches.status': 'Status',
    'admin.matches.players': 'Participants',
    'admin.matches.reservations': 'Workshop reservations',
    'admin.matches.noReservations': 'No reservations.',
    'admin.matches.hideDetails': 'Hide',
    'admin.matches.showDetails': 'View details',
    'admin.matches.convert': 'Open to public (consent required, D−2)',
    'admin.matches.convertSuccess': 'Workshop converted to public.',
    'admin.matches.loadError': 'Unable to load admin workshops.',
    'admin.matches.reservationsError': 'Unable to load workshop reservations.',
    'admin.matches.convertError': 'Unable to convert workshop.',
    'admin.matches.empty': 'No workshop available',
    'admin.closures.date': 'Date',
    'admin.closures.reason': 'Reason (optional)',
    'admin.closures.site': 'Center concerned',
    'admin.closures.noneGlobal': 'None (global)',
    'admin.closures.global': 'Global closure (all centers)',
    'admin.closures.add': 'Add closure',
    'admin.closures.reset': 'Reset',
    'admin.closures.reasonEmpty': 'No reason specified',
    'admin.closures.empty': 'No closure day configured',
    'common.notProvided': 'Not provided',
    'admin.home.allStatuses': 'all statuses',
    'admin.home.matchCount': 'workshop(es)',
    'admin.home.sitesVisible': 'Visible centers',
    'admin.home.terrainsVisible': 'Visible workshop rooms',
    'admin.home.completeMatches': 'Complete workshops',
    'admin.home.pendingReservations': 'Pending reservations',
    'admin.login.globalBadge': '🌐 Global admin',
    'admin.login.siteBadge': '🏟️ Center admin',
    'admin.login.showPassword': 'Show admin password',
    'admin.login.hidePassword': 'Hide admin password',
    'admin.members.searchNoResult': 'No result for "{query}"',
    'admin.members.loadError': 'Unable to load participants.',
    'admin.members.saveError': 'Unable to save participant.',
    'admin.members.deleteError': 'Unable to delete.',
    'admin.members.updateSuccess': 'Participant updated.',
    'admin.members.createSuccess': 'Participant created successfully.',
    'admin.members.deleteSuccess': 'Participant deleted.',
    'admin.members.deleteConfirm': 'Confirm deletion of this participant?',
    'admin.sites.name': 'Name',
    'admin.sites.address': 'Address',
    'admin.sites.opening': 'Opening',
    'admin.sites.closing': 'Closing',
    'admin.sites.matchDuration': 'Workshop duration (min)',
    'admin.sites.breakDuration': 'Break between workshops (min)',
    'admin.sites.civilYear': 'Calendar year',
    'admin.sites.openVisitors': 'Center open to visitors (booking up to 7 days ahead)',
    'admin.sites.hours': 'Hours',
    'admin.sites.duration': 'Workshop duration',
    'admin.sites.break': 'Break',
    'admin.sites.year': 'Year',
    'admin.sites.empty': 'No center configured',
    'admin.sites.siteAdminEditBlocked': 'A SITE admin cannot modify centers.',
    'admin.sites.siteAdminDeleteBlocked': 'A SITE admin cannot delete centers.',
    'admin.sites.loadError': 'Unable to load centers.',
    'admin.sites.saveError': 'Unable to save center.',
    'admin.sites.deleteError': 'Unable to delete center.',
    'admin.sites.deleteConfirm': 'Confirm deletion of this center?',
    'admin.sites.updateSuccess': 'Center updated.',
    'admin.sites.createSuccess': 'Center created.',
    'admin.sites.deleteSuccess': 'Center deleted.',
    'admin.terrains.name': 'Workshop room name',
    'admin.terrains.site': 'Center',
    'admin.terrains.empty': 'No workshop room configured',
    'admin.terrains.loadError': 'Unable to load workshop rooms.',
    'admin.terrains.saveError': 'Unable to save workshop room.',
    'admin.terrains.deleteError': 'Unable to delete workshop room.',
    'admin.terrains.deleteConfirm': 'Confirm deletion of this workshop room?',
    'admin.terrains.updateSuccess': 'Workshop room updated.',
    'admin.terrains.createSuccess': 'Workshop room created.',
    'admin.terrains.deleteSuccess': 'Workshop room deleted.',
    'admin.closures.loadError': 'Unable to load closure days.',
    'admin.closures.addSuccess': 'Closure day added.',
    'admin.closures.addError': 'Unable to add closure day.',
    'admin.closures.deleteSuccess': 'Closure day deleted.',
    'admin.closures.deleteError': 'Unable to delete closure day.',
    'member.home.matricule': 'Participant ID',
    'member.home.matriculePlaceholder': 'Ex: G1234, S12345, L12345',
    'member.home.showPassword': 'Show participant password',
    'member.home.hidePassword': 'Hide participant password',
    'member.home.globalHint': 'G + 4 digits · book up to 30 days ahead',
    'member.home.siteHint': 'S + 5 digits · book up to 15 days ahead at your center',
    'member.home.livreHint': 'L + 5 digits · book up to 7 days ahead',
    'member.home.allSites': 'All centers',
    'member.profile.loadError': 'Unable to load participant profile.',
    'member.create.bookingLimitError': 'Latest allowed date: {date}.',
    'member.create.bookingLimitProfileError': 'The {type} profile cannot book more than {days} days ahead. Deadline: {date}.',
    'member.create.invalidType': 'Invalid workshop type.',
    'member.create.createError': 'Unable to create workshop.',
    'member.create.outstandingBalance': 'You have an outstanding balance. Settle your debts before creating a workshop.',
    'member.create.activePenalty': 'You have an active penalty. Wait before creating a workshop.',
    'member.create.slotTaken': 'This slot is already booked. Pick another time.',
    'member.create.siteClosed': 'The center is closed on this date.',
    'member.create.outOfHours': "This time is outside the site's opening hours.",
    'member.create.siteOnly': 'Subscribers may create workshops only at their assigned center.',
    'member.create.internalError': 'An internal server error occurred while creating the workshop.',
    'member.create.addPlayersError': 'Unable to add all participants.',
    'member.public.loadError': 'Unable to load public workshops.',
    'member.public.noMember': 'No participant logged in.',
    'member.public.joinBlocked': 'This workshop cannot be joined (full, canceled, or not allowed).',
    'member.public.joinSuccess': 'Request created. Your spot will be confirmed after payment.',
    'member.public.joinError': 'Unable to reserve this workshop.',
    'member.public.modifyTooLate': 'Modification unavailable less than 24 hours before workshop start.',
    'member.public.updateSuccess': 'Workshop updated successfully.',
    'member.public.updateError': 'Unable to update workshop.',
    'member.public.deleteConfirm': 'Confirm deletion (cancellation) of workshop #{id}?',
    'member.public.deleteSuccess': 'Workshop canceled successfully.',
    'member.public.deleteError': 'Unable to delete workshop.',
    'member.reservations.reservationTitle': 'Reservation',
    'member.reservations.noMember': 'No participant logged in.',
    'member.reservations.loadError': 'Unable to load reservations.',
    'member.reservations.privateOnly': 'Participant management is reserved for PRIVATE workshops.',
    'member.reservations.playersLoadError': 'Unable to load workshop participants.',
    'member.reservations.updateSuccess': 'Workshop updated successfully.',
    'member.reservations.updateError': 'Unable to update workshop.',
    'member.reservations.deleteSuccess': 'Workshop canceled successfully.',
    'member.reservations.deleteError': 'Unable to delete workshop.',
    'member.reservations.playerAdded': 'Participant added successfully.',
    'member.reservations.playerAddError': 'Unable to add participant.',
    'member.reservations.playerRemoved': 'Participant removed from workshop.',
    'member.reservations.playerRemoveError': 'Unable to remove participant.',
    'member.reservations.paymentSuccess': 'Payment completed successfully.',
    'member.reservations.paymentError': 'Unable to process payment.',
    'member.reservations.reservationCanceled': 'Reservation canceled.',
    'member.reservations.reservationCancelError': 'Unable to cancel reservation.'
  }
};
