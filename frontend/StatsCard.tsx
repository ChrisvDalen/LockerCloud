export default function StatsCard() {
  return (
    <div className="bg-white rounded-xl shadow-md p-6">
      <h2 className="font-semibold mb-4">Statistieken</h2>
      <ul className="space-y-2 text-sm">
        <li className="flex justify-between">
          <span>Totaal bestanden:</span>
          <span className="bg-gray-100 rounded-full px-2">8</span>
        </li>
        <li className="flex justify-between">
          <span>Totale grootte:</span>
          <span className="bg-gray-100 rounded-full px-2">1.2 GB</span>
        </li>
        <li className="flex justify-between items-center">
          <span>Status:</span>
          <span className="bg-green-500 text-white rounded-full px-2 text-xs">Gesynchroniseerd</span>
        </li>
      </ul>
    </div>
  );
}
