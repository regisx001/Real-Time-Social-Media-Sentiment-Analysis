import { goto } from "$app/navigation";
import { redirect } from "@sveltejs/kit";
import type { PageLoad } from "./$types";


export const load: PageLoad = async ({ }) => {
    return redirect(307,"/dashboard");
}