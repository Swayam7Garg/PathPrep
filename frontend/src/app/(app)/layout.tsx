import { Sidebar } from "@/components/navigation/sidebar";
import { Header } from "@/components/navigation/header";
import { AuthGate } from "@/components/auth/auth-gate";

export default function AppLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <AuthGate>
      <div className="min-h-screen bg-[#FAFAFA]">
        <Sidebar />
        <div className="pl-60">
          <Header />
          <main className="pt-16 min-h-[calc(100vh-4rem)] bg-[#FAFAFA]">
            {children}
          </main>
        </div>
      </div>
    </AuthGate>
  );
}
