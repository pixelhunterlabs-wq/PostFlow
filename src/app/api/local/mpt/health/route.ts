import { NextResponse } from "next/server";
import { getMptHealth, MptUnavailableError } from "@/lib/moneyprinterturbo";

export const runtime = "nodejs";

export async function GET() {
  try {
    return NextResponse.json(await getMptHealth());
  } catch (error) {
    const message = error instanceof Error ? error.message : "MoneyPrinterTurbo kontrol edilemedi.";
    const status = error instanceof MptUnavailableError ? error.status : 500;
    return NextResponse.json({ ok: false, error: message }, { status });
  }
}
