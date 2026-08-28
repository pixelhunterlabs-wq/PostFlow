import { NextRequest, NextResponse } from "next/server";
import { getMptTask, MptUnavailableError } from "@/lib/moneyprinterturbo";

export const runtime = "nodejs";

export async function GET(_: NextRequest, context: { params: Promise<{ taskId: string }> }) {
  try {
    const { taskId } = await context.params;
    return NextResponse.json(await getMptTask(taskId));
  } catch (error) {
    const message = error instanceof Error ? error.message : "MoneyPrinterTurbo görev durumu okunamadı.";
    const status = error instanceof MptUnavailableError ? error.status : 500;
    return NextResponse.json({ error: message }, { status });
  }
}
