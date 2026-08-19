import { getProfile } from '../../api/profile';
import PageHeader from '../../components/PageHeader';
import { useResource } from '../../hooks/useResource';
import PersonalInfoForm from './PersonalInfoForm';
import CvSection from './CvSection';
import ExperienceSection from './ExperienceSection';
import FormationSection from './FormationSection';
import SkillSection from './SkillSection';
import DegreeSection from './DegreeSection';
import LanguageSection from './LanguageSection';

/** Page « Mon profil » de l'espace intérimaire (incrément 3a). */
export default function ProfilePage() {
  const {
    data: profile,
    setData: setProfile,
    loading,
    error,
    reload,
  } = useResource(getProfile, 'Impossible de charger le profil.');

  if (loading) {
    return <p className="text-slate-500">Chargement du profil…</p>;
  }

  if (error && !profile) {
    return <p className="rounded-md bg-red-50 px-3 py-2 text-sm text-red-700">{error}</p>;
  }

  if (!profile) {
    return null;
  }

  return (
    <div className="space-y-6">
      <PageHeader title="Mon profil" subtitle={profile.email} />

      <PersonalInfoForm profile={profile} onSaved={setProfile} />
      <CvSection cvFilePath={profile.cvFilePath} onChanged={reload} />
      <ExperienceSection experiences={profile.experiences} onChanged={reload} />
      <FormationSection formations={profile.formations} onChanged={reload} />
      <SkillSection />
      <DegreeSection />
      <LanguageSection />
    </div>
  );
}
