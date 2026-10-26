import axios from "axios";

const baseURL = (import.meta.env.VITE_API_URL as string | undefined) ?? "/api/v1";

const api = axios.create({
  baseURL,
  headers: { "Content-Type": "application/json" },
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem("accessToken");
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const original = error.config;
    if (error.response?.status === 401 && !original._retry) {
      original._retry = true;
      const refreshToken = localStorage.getItem("refreshToken");
      if (refreshToken) {
        try {
          const res = await axios.post(`${baseURL}/admin/auth/refresh`, { refreshToken });
          const { accessToken, refreshToken: newRefresh } = res.data.data;
          localStorage.setItem("accessToken", accessToken);
          localStorage.setItem("refreshToken", newRefresh);
          original.headers.Authorization = `Bearer ${accessToken}`;
          return api(original);
        } catch {
          localStorage.clear();
          window.location.href = "/login";
        }
      }
    }
    return Promise.reject(error);
  }
);

export default api;

export interface ApiResponse<T> {
  success: boolean;
  code: string;
  message?: string;
  data: T;
  reference?: string;
  timestamp: string;
}

export interface PagedResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}
