import Navbar from './Navbar';
import UploadCard from './UploadCard';
import FilesTable from './FilesTable';
import StatsCard from './StatsCard';
import QuickActions from './QuickActions';

export default function App() {
  return (
    <div className="min-h-screen font-sans bg-gradient-to-b from-[#F5F9FF] to-white">
      <Navbar />
      <main className="max-w-6xl mx-auto mt-20 px-4 grid gap-6 lg:grid-cols-[2fr_1fr]">
        <div className="space-y-6">
          <UploadCard />
          <FilesTable />
        </div>
        <div className="space-y-6">
          <StatsCard />
          <QuickActions />
        </div>
      </main>
    </div>
  );
}
