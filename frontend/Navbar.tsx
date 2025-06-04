import { useState } from 'react';
import { CloudIcon, HomeIcon, FolderIcon, Cog6ToothIcon, Bars3Icon, XMarkIcon } from '@heroicons/react/24/outline';

export default function Navbar() {
  const [open, setOpen] = useState(false);

  return (
    <nav className="fixed top-0 inset-x-0 h-14 bg-[#1E6EF2] text-white flex items-center px-4 shadow-md z-10">
      <div className="flex items-center flex-1">
        <CloudIcon className="h-6 w-6 mr-2" />
        <span className="font-semibold">CloudLocker</span>
      </div>
      <button className="sm:hidden" onClick={() => setOpen(!open)}>
        {open ? <XMarkIcon className="h-6 w-6" /> : <Bars3Icon className="h-6 w-6" />}
      </button>
      <ul className={`sm:flex space-x-6 items-center ${open ? 'block absolute top-14 right-0 bg-[#1E6EF2] w-48 p-4' : 'hidden'} sm:static sm:w-auto sm:p-0 sm:block`}>
        <li className="flex items-center space-x-1 cursor-pointer">
          <HomeIcon className="h-5 w-5" />
          <span>Home</span>
        </li>
        <li className="flex items-center space-x-1 cursor-pointer border-b-2 border-white">
          <FolderIcon className="h-5 w-5" />
          <span>Bestanden</span>
        </li>
        <li className="flex items-center space-x-1 cursor-pointer">
          <Cog6ToothIcon className="h-5 w-5" />
          <span>Instellingen</span>
        </li>
        <li className="ml-2">
          <span className="bg-white text-[#1E6EF2] rounded-full px-2 py-0.5 text-xs font-semibold">80 %</span>
        </li>
      </ul>
    </nav>
  );
}
