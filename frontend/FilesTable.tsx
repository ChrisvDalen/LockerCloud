import { useState } from 'react';
import { ArrowDownTrayIcon, TrashIcon, CheckCircleIcon } from '@heroicons/react/24/outline';

interface FileRow {
  name: string;
  size: string;
  type: 'image' | 'binary' | 'pdf' | 'text';
}

const initialFiles: FileRow[] = [
  { name: '1336screenshotDiefstal - Copy (3).png', size: '2.3 MB', type: 'image' },
  { name: '1GB.bin', size: '1 GB', type: 'binary' },
  { name: 'CV Aart Schouten.pdf', size: '156 KB', type: 'pdf' },
  { name: 'test.txt', size: '1 KB', type: 'text' },
];

export default function FilesTable() {
  const [files, setFiles] = useState<FileRow[]>(initialFiles);

  const handleDownload = (file: FileRow) => {
    alert(`Download ${file.name}`);
  };

  const handleDelete = (file: FileRow) => {
    setFiles(files.filter(f => f !== file));
  };

  const badgeColor = (type: FileRow['type']) => {
    switch (type) {
      case 'image':
        return 'bg-blue-500';
      case 'binary':
        return 'bg-purple-500';
      case 'pdf':
        return 'bg-rose-500';
      case 'text':
        return 'bg-slate-500';
    }
  };

  return (
    <div className="bg-white rounded-xl shadow-md p-6 overflow-auto">
      <div className="flex justify-between items-center mb-4">
        <h2 className="font-semibold">Bestanden op de server</h2>
        <div className="space-x-2">
          <button
            className="bg-[#1E6EF2] text-white px-3 py-1 rounded-md text-sm"
            onClick={() => alert('Synchroniseren gestart')}
          >
            Synchroniseren
          </button>
          <button className="border border-[#1E6EF2] text-[#1E6EF2] px-3 py-1 rounded-md text-sm">
            Download alles
          </button>
        </div>
      </div>
      <table className="w-full text-sm">
        <thead>
          <tr className="text-left text-gray-500">
            <th className="py-2">Bestandsnaam</th>
            <th className="py-2">Grootte</th>
            <th className="py-2">Type</th>
            <th className="py-2">Acties</th>
          </tr>
        </thead>
        <tbody>
          {files.map(file => (
            <tr key={file.name} className="border-t">
              <td className="py-2 pr-2">{file.name}</td>
              <td className="py-2 pr-2">{file.size}</td>
              <td className="py-2 pr-2">
                <span className={`text-white text-xs px-2 py-0.5 rounded-full ${badgeColor(file.type)}`}>{file.type}</span>
              </td>
              <td className="py-2 flex space-x-2">
                <CheckCircleIcon className="h-5 w-5 text-green-500" />
                <ArrowDownTrayIcon
                  className="h-5 w-5 text-green-500 cursor-pointer hover:opacity-80"
                  onClick={() => handleDownload(file)}
                />
                <TrashIcon
                  className="h-5 w-5 text-red-500 cursor-pointer hover:opacity-80"
                  onClick={() => handleDelete(file)}
                />
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
