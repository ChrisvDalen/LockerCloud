import { useRef } from 'react';
import { CloudArrowUpIcon } from '@heroicons/react/24/outline';

export default function UploadCard() {
  const inputRef = useRef<HTMLInputElement | null>(null);

  const handleClick = () => {
    inputRef.current?.click();
  };

  const handleFiles = (e: React.ChangeEvent<HTMLInputElement>) => {
    console.log(e.target.files);
  };

  return (
    <div className="bg-white rounded-xl shadow-md p-6 flex flex-col items-center justify-center">
      <div
        className="border-2 border-dashed border-gray-300 rounded-lg h-48 w-full flex flex-col items-center justify-center cursor-pointer"
        onClick={handleClick}
      >
        <CloudArrowUpIcon className="h-12 w-12 text-gray-400" />
        <p className="mt-2 text-gray-500 text-sm text-center">
          Sleep bestanden hierheen of klik om te selecteren
        </p>
        <input
          type="file"
          multiple
          className="hidden"
          ref={inputRef}
          onChange={handleFiles}
        />
      </div>
    </div>
  );
}
