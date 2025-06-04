import { ArrowDownTrayIcon, CloudArrowUpIcon, Cog6ToothIcon } from '@heroicons/react/24/outline';

export default function QuickActions() {
  return (
    <div className="bg-white rounded-xl shadow-md p-6">
      <h2 className="font-semibold mb-4">Snelle acties</h2>
      <div className="space-y-2">
        <button className="w-full flex items-center justify-center border border-gray-300 rounded-md py-2 hover:bg-gray-50">
          <ArrowDownTrayIcon className="h-5 w-5 mr-2" />
          Download alle bestanden
        </button>
        <button className="w-full flex items-center justify-center border border-gray-300 rounded-md py-2 hover:bg-gray-50">
          <CloudArrowUpIcon className="h-5 w-5 mr-2" />
          Bulk upload
        </button>
        <button className="w-full flex items-center justify-center border border-gray-300 rounded-md py-2 hover:bg-gray-50">
          <Cog6ToothIcon className="h-5 w-5 mr-2" />
          Synchronisatie instellingen
        </button>
      </div>
    </div>
  );
}
